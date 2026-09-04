package com.cryptostrategy.platform.persistence.internal.search;

import com.cryptostrategy.platform.execution.api.ReproductionVerificationId;

import com.cryptostrategy.platform.execution.api.port.out.SearchReproductionVerificationGateway;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** PostgreSQL claim/recovery fence cho verification PENDING/RUNNING. */
public final class JdbcSearchReproductionVerificationGateway
        implements SearchReproductionVerificationGateway {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final ObjectMapper json = new ObjectMapper();

    public JdbcSearchReproductionVerificationGateway(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource");
        jdbc = new JdbcTemplate(dataSource);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Override
    public List<ExperimentId> findReady(int limit) {
        return jdbc.query("""
                select rv.reproduction_experiment_id from search.reproduction_verification rv
                join experiment.experiment e on e.experiment_id=rv.reproduction_experiment_id
                where rv.status in ('PENDING','RUNNING') and e.status in ('COMPLETED','STOPPED')
                order by rv.updated_at,rv.verification_id limit ?
                """, (rs, row) -> new ExperimentId(rs.getString(1)), limit);
    }

    @Override
    public Optional<Work> claimReady(ExperimentId reproductionExperimentId, Instant now) {
        return transaction.execute(status -> {
            List<Row> found = jdbc.query("""
                    select rv.verification_id,rv.version,rv.status,rv.source_experiment_id,
                        rv.reproduction_experiment_id,e.owner_user_id,e.status experiment_status
                    from search.reproduction_verification rv
                    join experiment.experiment e on e.experiment_id=rv.reproduction_experiment_id
                    where rv.reproduction_experiment_id=? for update of rv
                    """, (rs, row) -> new Row(rs.getString("verification_id"), rs.getLong("version"),
                    rs.getString("status"), rs.getString("source_experiment_id"),
                    rs.getString("reproduction_experiment_id"), rs.getObject("owner_user_id", UUID.class),
                    rs.getString("experiment_status")), reproductionExperimentId.value());
            if (found.isEmpty()) return Optional.empty();
            Row row = found.getFirst();
            if (!List.of("COMPLETED", "STOPPED").contains(row.experimentStatus())
                    || !List.of("PENDING", "RUNNING").contains(row.status())) return Optional.empty();
            long claimedVersion = row.version();
            if ("PENDING".equals(row.status())) {
                int updated = jdbc.update("""
                        update search.reproduction_verification set status='RUNNING',version=version+1,
                            started_at=?,updated_at=? where verification_id=? and version=? and status='PENDING'
                        """, Timestamp.from(now), Timestamp.from(now), row.id(), row.version());
                if (updated != 1) return Optional.empty();
                claimedVersion++;
            }
            return Optional.of(new Work(new ReproductionVerificationId(row.id()), claimedVersion, row.owner(),
                    new ExperimentId(row.source()), new ExperimentId(row.reproduction())));
        });
    }

    @Override
    public boolean complete(Completion value) {
        try {
            if ("FAILED".equals(value.status())) {
                return jdbc.update("""
                        update search.reproduction_verification set status='FAILED',version=version+1,
                            failure_code=?,failure_message=?,finished_at=?,updated_at=?
                        where verification_id=? and version=? and status='RUNNING'
                        """, value.failureCode(), value.failureMessage(), Timestamp.from(value.finishedAt()),
                        Timestamp.from(value.finishedAt()), value.verificationId().value(), value.expectedVersion()) == 1;
            }
            return jdbc.update("""
                    update search.reproduction_verification set status=?,version=version+1,
                        trade_sequence_matched=?,metrics_matched=?,fingerprints_matched=?,
                        source_evidence_fingerprint=?,reproduction_evidence_fingerprint=?,safe_differences=?::jsonb,
                        failure_code=?,failure_message=?,finished_at=?,updated_at=?
                    where verification_id=? and version=? and status='RUNNING'
                    """, value.status(), value.tradesMatched(), value.metricsMatched(), value.fingerprintsMatched(),
                    value.sourceFingerprint(), value.reproductionFingerprint(),
                    json.writeValueAsString(value.safeDifferences()), value.failureCode(), value.failureMessage(),
                    Timestamp.from(value.finishedAt()), Timestamp.from(value.finishedAt()), value.verificationId().value(),
                    value.expectedVersion()) == 1;
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot persist reproduction verification", failure);
        }
    }

    private record Row(String id, long version, String status, String source, String reproduction,
            UUID owner, String experimentStatus) {}
}

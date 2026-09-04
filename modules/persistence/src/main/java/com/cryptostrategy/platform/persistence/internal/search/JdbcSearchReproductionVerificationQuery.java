package com.cryptostrategy.platform.persistence.internal.search;

import com.cryptostrategy.platform.execution.api.port.in.GetSearchReproductionVerificationUseCase;
import com.cryptostrategy.platform.execution.api.ReproductionVerificationId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** PostgreSQL owner-scoped projection for public reproduction reconciliation. */
public final class JdbcSearchReproductionVerificationQuery
        implements GetSearchReproductionVerificationUseCase {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcSearchReproductionVerificationQuery(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.json = Objects.requireNonNull(json, "json");
    }

    @Override
    public Optional<Snapshot> get(UUID ownerUserId, ExperimentId reproductionExperimentId) {
        return jdbc.query("""
                select rv.verification_id,rv.source_experiment_id,rv.reproduction_experiment_id,
                    rv.status,rv.trade_sequence_matched,rv.metrics_matched,rv.fingerprints_matched,
                    rv.source_evidence_fingerprint,rv.reproduction_evidence_fingerprint,
                    rv.safe_differences::text,rv.failure_code,rv.failure_message,
                    rv.started_at,rv.finished_at,rv.updated_at
                from search.reproduction_verification rv
                join experiment.experiment e on e.experiment_id=rv.reproduction_experiment_id
                where rv.reproduction_experiment_id=? and e.owner_user_id=?
                """, (rs, row) -> new Snapshot(
                        new ReproductionVerificationId(rs.getString("verification_id")),
                        new ExperimentId(rs.getString("source_experiment_id")),
                        new ExperimentId(rs.getString("reproduction_experiment_id")),
                        rs.getString("status"),
                        (Boolean) rs.getObject("trade_sequence_matched"),
                        (Boolean) rs.getObject("metrics_matched"),
                        (Boolean) rs.getObject("fingerprints_matched"),
                        rs.getString("source_evidence_fingerprint"),
                        rs.getString("reproduction_evidence_fingerprint"),
                        differences(rs.getString("safe_differences")),
                        rs.getString("failure_code"),
                        rs.getString("failure_message"),
                        instant(rs.getTimestamp("started_at")),
                        instant(rs.getTimestamp("finished_at")),
                        rs.getTimestamp("updated_at").toInstant()),
                reproductionExperimentId.value(), ownerUserId).stream().findFirst();
    }

    private Map<String, Object> differences(String value) {
        if (value == null) return Map.of();
        try {
            return json.readValue(value, new TypeReference<>() {});
        } catch (Exception invalid) {
            throw new IllegalStateException("Stored reproduction differences are invalid", invalid);
        }
    }

    private static java.time.Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}

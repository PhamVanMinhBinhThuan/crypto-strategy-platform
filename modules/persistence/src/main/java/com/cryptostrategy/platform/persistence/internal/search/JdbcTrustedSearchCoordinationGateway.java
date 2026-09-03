package com.cryptostrategy.platform.persistence.internal.search;

import com.cryptostrategy.platform.execution.api.port.out.TrustedSearchCoordinationGateway;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** PostgreSQL authority cho progress/lifecycle Search, với cùng version fence cho Run và SEARCH Job. */
public final class JdbcTrustedSearchCoordinationGateway implements TrustedSearchCoordinationGateway {
    private static final String SNAPSHOT = "select " + SearchSql.COLUMNS + """
            , (select count(*) from experiment.candidate_definition c
               where c.experiment_id=sr.experiment_id) allocated_work
            , (select count(*) from experiment.job j
               where j.experiment_id=sr.experiment_id and j.job_type='BACKTEST' and j.status='SUCCEEDED') completed_work
            , (select count(*) from experiment.job j
               where j.experiment_id=sr.experiment_id and j.job_type='BACKTEST' and j.status in ('FAILED','CANCELLED')) failed_work
            , (select max(br.completed_at) from experiment.backtest_result br
               where br.experiment_id=sr.experiment_id) latest_completed_at
            from search.search_run sr where sr.experiment_id=?
            """;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final SearchRows rows = new SearchRows();

    public JdbcTrustedSearchCoordinationGateway(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource");
        this.jdbc = new JdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Override
    public Optional<AuthoritativeSnapshot> load(String experimentId) {
        List<AuthoritativeSnapshot> found = jdbc.query(SNAPSHOT, this::map, experimentId);
        return found.stream().findFirst();
    }

    @Override
    public Optional<AuthoritativeSnapshot> loadCompletion(
            String experimentId, String candidateId, String backtestJobId) {
        Integer valid = jdbc.queryForObject("""
                select count(*) from experiment.candidate_definition c
                join experiment.job j on j.candidate_id=c.candidate_id and j.experiment_id=c.experiment_id
                join experiment.backtest_result br on br.job_id=j.job_id and br.experiment_id=j.experiment_id
                join experiment.evaluation_result er on er.backtest_result_id=br.backtest_result_id
                where c.experiment_id=? and c.candidate_id=? and j.job_id=? and j.status='SUCCEEDED'
                """, Integer.class, experimentId, candidateId, backtestJobId);
        return valid != null && valid > 0 ? load(experimentId) : Optional.empty();
    }

    @Override
    public boolean commit(Transition change) {
        return Boolean.TRUE.equals(transaction.execute(status -> commitInTransaction(change)));
    }

    private boolean commitInTransaction(Transition change) {
        if (change.processedMessageRef() != null) {
            int inserted = jdbc.update("""
                    insert into platform.processed_message(consumer_name,message_id,processed_at,expires_at)
                    values ('search-coordinators',?,?,?) on conflict do nothing
                    """, change.processedMessageRef(), Timestamp.from(change.replacement().updatedAt()),
                    Timestamp.from(change.replacement().updatedAt().plus(Duration.ofDays(7))));
            if (inserted == 0) return true;
        }
        SearchRun run = change.replacement();
        int updated = jdbc.update(SearchSql.UPDATE_FENCED,
                run.generatorState().contractVersion(), run.generatorState().canonicalState(),
                run.generatorState().fingerprint(), run.nextGenerationIndex(), run.status().name(), run.version(),
                timestamp(run.startedAt()), timestamp(run.deadlineAt()), timestamp(run.finishedAt()),
                run.failureCode(), run.failureMessage(), Timestamp.from(run.updatedAt()),
                run.searchRunId().value(), change.expectedVersion());
        if (updated != 1) return false;
        String jobStatus = switch (run.status()) {
            case COMPLETED -> "SUCCEEDED";
            case STOPPED -> "CANCELLED";
            case FAILED -> "FAILED";
            case PENDING -> "QUEUED";
            default -> "RUNNING";
        };
        jdbc.update("""
                update experiment.job set completed_work=?, failed_work=?, status=?,
                    started_at=coalesce(started_at,?), finished_at=?, updated_at=?
                where job_id=? and experiment_id=? and job_type='SEARCH'
                """, change.completedWork(), change.failedWork(), jobStatus,
                timestamp(run.startedAt()), run.status().isTerminal() ? timestamp(run.finishedAt()) : null,
                Timestamp.from(run.updatedAt()), run.searchJobRef(), run.experimentRef());
        return true;
    }

    private AuthoritativeSnapshot map(ResultSet rs, int row) throws SQLException {
        Timestamp latest = rs.getTimestamp("latest_completed_at");
        return new AuthoritativeSnapshot(rows.mapSearchRun(rs, row), rs.getInt("allocated_work"),
                rs.getInt("completed_work"), rs.getInt("failed_work"),
                latest == null ? null : latest.toInstant());
    }

    private static Timestamp timestamp(java.time.Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}

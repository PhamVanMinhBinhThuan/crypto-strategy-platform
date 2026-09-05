package com.cryptostrategy.platform.persistence.internal.search;

import com.cryptostrategy.platform.execution.api.port.out.TrustedSearchCoordinationGateway;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunStatus;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
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
    private static final String SNAPSHOT = "select sr.*" + """
            , progress.allocated_work, progress.completed_work, progress.failed_work,
              progress.latest_completed_at,
              coalesce((manifest.search_config ->> 'maximumCandidates')::integer,
                       sr.maximum_candidates) configured_maximum_candidates,
              progress.settled_prefix - coalesce(best.generation_index + 1, 0)
                  consecutive_without_improvement
            from (select
            """ + SearchSql.COLUMNS + """
                  from search.search_run where experiment_id=?) sr
            join experiment.experiment_manifest manifest on manifest.experiment_id=sr.experiment_id
            cross join lateral (
                select count(c.candidate_id)::integer allocated_work,
                    count(*) filter (where j.status='SUCCEEDED')::integer completed_work,
                    count(*) filter (where j.status in ('FAILED','CANCELLED'))::integer failed_work,
                    max(br.completed_at) latest_completed_at,
                    coalesce(min(c.generation_index) filter (
                        where j.status not in ('SUCCEEDED','FAILED','CANCELLED')),
                        count(c.candidate_id)::integer) settled_prefix
                from experiment.candidate_definition c
                left join experiment.job j on j.candidate_id=c.candidate_id
                    and j.experiment_id=c.experiment_id and j.job_type='BACKTEST'
                left join experiment.backtest_result br on br.job_id=j.job_id
                where c.experiment_id=sr.experiment_id
            ) progress
            left join lateral (
                select c.generation_index
                from experiment.candidate_definition c
                join experiment.job j on j.candidate_id=c.candidate_id
                    and j.experiment_id=c.experiment_id and j.status='SUCCEEDED'
                join experiment.backtest_result br on br.job_id=j.job_id
                join experiment.evaluation_result er on er.backtest_result_id=br.backtest_result_id
                where c.experiment_id=sr.experiment_id
                  and c.generation_index < progress.settled_prefix
                order by er.overall_score desc, c.generation_index asc
                limit 1
            ) best on true
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
    public Optional<AuthoritativeSnapshot> load(ExperimentId experimentId) {
        List<AuthoritativeSnapshot> found = jdbc.query(SNAPSHOT, this::map, experimentId.value());
        return found.stream().findFirst();
    }

    @Override
    public Optional<AuthoritativeSnapshot> loadCompletion(
            ExperimentId experimentId, CandidateId candidateId, JobId backtestJobId) {
        Integer valid = jdbc.queryForObject("""
                select count(*) from experiment.candidate_definition c
                join experiment.job j on j.candidate_id=c.candidate_id and j.experiment_id=c.experiment_id
                join experiment.backtest_result br on br.job_id=j.job_id and br.experiment_id=j.experiment_id
                join experiment.evaluation_result er on er.backtest_result_id=br.backtest_result_id
                where c.experiment_id=? and c.candidate_id=? and j.job_id=? and j.status='SUCCEEDED'
                """, Integer.class, experimentId.value(), candidateId.value(), backtestJobId.value());
        return valid != null && valid > 0 ? load(experimentId) : Optional.empty();
    }

    @Override
    public boolean commit(Transition change) {
        return Boolean.TRUE.equals(transaction.execute(status -> commitInTransaction(change)));
    }

    private boolean commitInTransaction(Transition change) {
        if (change.messageId() != null) {
            int inserted = jdbc.update("""
                    insert into platform.processed_message(consumer_name,message_id,processed_at,expires_at)
                    values ('search-coordinators',?,?,?) on conflict do nothing
                    """, change.messageId(), Timestamp.from(change.replacement().updatedAt()),
                    Timestamp.from(change.replacement().updatedAt().plus(Duration.ofDays(7))));
            if (inserted == 0) return true;
        }
        SearchRun run = change.replacement();
        int updated = jdbc.update(SearchSql.UPDATE_FENCED,
                run.generatorState().contractVersion(), run.generatorState().canonicalState(),
                run.generatorState().fingerprint(), run.nextGenerationIndex(), run.status().name(),
                run.terminalReason() == null ? null : run.terminalReason().name(), run.version(),
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
                Timestamp.from(run.updatedAt()), run.searchJobId().value(), run.experimentId().value());
        String experimentStatus = switch (run.status()) {
            case PENDING -> "QUEUED";
            case RUNNING -> "RUNNING";
            case STOPPING -> "STOP_REQUESTED";
            case COMPLETED -> "COMPLETED";
            case STOPPED -> "STOPPED";
            case FAILED -> "FAILED";
        };
        jdbc.update("""
                update experiment.experiment set status=?,
                    started_at=coalesce(started_at,?), completed_at=?,
                    failure_code=?, failure_message=? where experiment_id=?
                """, experimentStatus, timestamp(run.startedAt()),
                run.status().isTerminal() ? timestamp(run.finishedAt()) : null,
                run.failureCode(), run.failureMessage(), run.experimentId().value());
        return true;
    }

    private AuthoritativeSnapshot map(ResultSet rs, int row) throws SQLException {
        Timestamp latest = rs.getTimestamp("latest_completed_at");
        return new AuthoritativeSnapshot(rows.mapSearchRun(rs, row), rs.getInt("allocated_work"),
                rs.getInt("completed_work"), rs.getInt("failed_work"),
                latest == null ? null : latest.toInstant(),
                rs.getInt("configured_maximum_candidates"),
                rs.getInt("consecutive_without_improvement"));
    }

    private static Timestamp timestamp(java.time.Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}

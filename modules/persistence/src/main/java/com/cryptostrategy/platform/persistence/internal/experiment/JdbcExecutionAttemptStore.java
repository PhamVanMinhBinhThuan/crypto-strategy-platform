package com.cryptostrategy.platform.persistence.internal.experiment;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.AttemptStatus;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import com.cryptostrategy.platform.experiment.api.job.StaleRunningAttempt;
import com.cryptostrategy.platform.experiment.api.port.out.ExecutionAttemptStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class JdbcExecutionAttemptStore implements ExecutionAttemptStore {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ExperimentRows rows;

    public JdbcExecutionAttemptStore(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            ExperimentRows rows
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate cannot be null");
        this.rows = Objects.requireNonNull(rows, "rows cannot be null");
    }

    @Override
    public ExecutionAttempt startNextAttempt(UUID ownerUserId, JobId jobId, String workerId, Instant startTime) {
        return transactionTemplate.execute(status -> {
            // 1. Lock the parent Job row
            Job job = jdbcTemplate.queryForObject(
                    ExperimentSql.SELECT_JOB_BY_ID_FOR_UPDATE,
                    rows::mapJob,
                    jobId.value(),
                    ownerUserId
            );
            if (job == null) {
                throw new IllegalStateException("Parent job not found for jobId: " + jobId);
            }

            // 2. Compute next attempt number
            Integer maxAttempt = jdbcTemplate.queryForObject(
                    ExperimentSql.SELECT_MAX_ATTEMPT_NO,
                    Integer.class,
                    jobId.value()
            );
            int nextAttemptNo = (maxAttempt != null ? maxAttempt : 0) + 1;

            // 3. Insert Execution Attempt
            AttemptId attemptId = AttemptId.generate();
            CandidateId candidateId = job.candidateId();
            if (candidateId == null) {
                throw new IllegalStateException("Cannot start execution attempt on search job without candidate");
            }

            jdbcTemplate.update(
                    ExperimentSql.INSERT_ATTEMPT,
                    attemptId.value(),
                    jobId.value(),
                    candidateId.value(),
                    nextAttemptNo,
                    AttemptStatus.RUNNING.name(),
                    workerId,
                    toTimestamp(startTime),
                    null,
                    null,
                    null,
                    null,
                    false,
                    toTimestamp(startTime)
            );

            // 4. Update parent Job status to RUNNING
            jdbcTemplate.update(
                    ExperimentSql.UPDATE_JOB_STATUS,
                    JobStatus.RUNNING.name(),
                    toTimestamp(startTime),
                    null,
                    null,
                    null,
                    null,
                    toTimestamp(startTime),
                    jobId.value(),
                    ownerUserId
            );

            return new ExecutionAttempt(
                    attemptId,
                    jobId,
                    candidateId,
                    nextAttemptNo,
                    AttemptStatus.RUNNING,
                    workerId,
                    startTime,
                    null,
                    null,
                    null,
                    null,
                    false,
                    startTime
            );
        });
    }

    @Override
    public boolean finalizeAttemptSuccess(UUID ownerUserId, JobId jobId, AttemptId attemptId, Instant finishTime) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            int attemptRows = jdbcTemplate.update(
                    ExperimentSql.UPDATE_ATTEMPT_STATUS_GUARDED,
                    AttemptStatus.SUCCEEDED.name(),
                    toTimestamp(finishTime),
                    null,
                    null,
                    null,
                    false,
                    attemptId.value(),
                    jobId.value(),
                    ownerUserId
            );
            if (attemptRows == 0) {
                return false;
            }

            int jobRows = jdbcTemplate.update(
                    ExperimentSql.UPDATE_JOB_STATUS_GUARDED,
                    JobStatus.SUCCEEDED.name(),
                    null,
                    toTimestamp(finishTime),
                    null,
                    null,
                    null,
                    toTimestamp(finishTime),
                    jobId.value(),
                    ownerUserId
            );
            if (jobRows == 0) {
                status.setRollbackOnly();
                return false;
            }
            return true;
        }));
    }

    @Override
    public boolean finalizeAttemptRetryableFailure(
            UUID ownerUserId,
            JobId jobId,
            AttemptId attemptId,
            String failureCode,
            String failureMessage,
            Instant finishTime,
            Instant nextRetryTime
    ) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            int attemptRows = jdbcTemplate.update(
                    ExperimentSql.UPDATE_ATTEMPT_STATUS_GUARDED,
                    AttemptStatus.FAILED.name(),
                    toTimestamp(finishTime),
                    toTimestamp(nextRetryTime),
                    failureCode,
                    failureMessage,
                    true,
                    attemptId.value(),
                    jobId.value(),
                    ownerUserId
            );
            if (attemptRows == 0) {
                return false;
            }

            int jobRows = jdbcTemplate.update(
                    ExperimentSql.UPDATE_JOB_STATUS_GUARDED,
                    JobStatus.RETRY_SCHEDULED.name(),
                    null,
                    toTimestamp(finishTime),
                    toTimestamp(nextRetryTime),
                    failureCode,
                    failureMessage,
                    toTimestamp(finishTime),
                    jobId.value(),
                    ownerUserId
            );
            if (jobRows == 0) {
                status.setRollbackOnly();
                return false;
            }
            return true;
        }));
    }

    @Override
    public boolean finalizeAttemptTerminalFailure(
            UUID ownerUserId,
            JobId jobId,
            AttemptId attemptId,
            String failureCode,
            String failureMessage,
            Instant finishTime
    ) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            int attemptRows = jdbcTemplate.update(
                    ExperimentSql.UPDATE_ATTEMPT_STATUS_GUARDED,
                    AttemptStatus.FAILED.name(),
                    toTimestamp(finishTime),
                    null,
                    failureCode,
                    failureMessage,
                    false,
                    attemptId.value(),
                    jobId.value(),
                    ownerUserId
            );
            if (attemptRows == 0) {
                return false;
            }

            int jobRows = jdbcTemplate.update(
                    ExperimentSql.UPDATE_JOB_STATUS_GUARDED,
                    JobStatus.FAILED.name(),
                    null,
                    toTimestamp(finishTime),
                    null,
                    failureCode,
                    failureMessage,
                    toTimestamp(finishTime),
                    jobId.value(),
                    ownerUserId
            );
            if (jobRows == 0) {
                status.setRollbackOnly();
                return false;
            }
            return true;
        }));
    }

    @Override
    public boolean finalizeAttemptCancelled(UUID ownerUserId, JobId jobId, AttemptId attemptId, Instant finishTime) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            int attemptRows = jdbcTemplate.update(
                    ExperimentSql.UPDATE_ATTEMPT_STATUS_GUARDED,
                    AttemptStatus.CANCELLED.name(),
                    toTimestamp(finishTime),
                    null,
                    null,
                    null,
                    false,
                    attemptId.value(),
                    jobId.value(),
                    ownerUserId
            );
            if (attemptRows == 0) {
                return false;
            }

            int jobRows = jdbcTemplate.update(
                    ExperimentSql.UPDATE_JOB_STATUS_CANCEL_GUARDED,
                    toTimestamp(finishTime),
                    toTimestamp(finishTime),
                    jobId.value(),
                    ownerUserId
            );
            if (jobRows == 0) {
                status.setRollbackOnly();
                return false;
            }
            return true;
        }));
    }

    @Override
    public List<ExecutionAttempt> listAttemptsByJobId(UUID ownerUserId, JobId jobId) {
        return jdbcTemplate.query(
                ExperimentSql.SELECT_ATTEMPTS_BY_JOB_ID,
                rows::mapAttempt,
                jobId.value(),
                ownerUserId
        );
    }

    @Override
    public List<StaleRunningAttempt> findStaleRunningAttempts(Instant startedBefore, int limit) {
        return jdbcTemplate.query(
                ExperimentSql.SELECT_STALE_RUNNING_ATTEMPTS,
                (rs, rowNum) -> new StaleRunningAttempt(
                        new JobId(rs.getString("job_id")),
                        new AttemptId(rs.getString("attempt_id")),
                        new ExperimentId(rs.getString("experiment_id")),
                        new CandidateId(rs.getString("candidate_id")),
                        rs.getString("worker_id"),
                        rs.getInt("attempt_no"),
                        rs.getTimestamp("started_at").toInstant()
                ),
                toTimestamp(startedBefore),
                limit
        );
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }
}

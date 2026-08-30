package com.cryptostrategy.platform.persistence.internal.experiment;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.AttemptStatus;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
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
    public void finalizeAttemptSuccess(UUID ownerUserId, JobId jobId, AttemptId attemptId, Instant finishTime) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update(
                    ExperimentSql.UPDATE_ATTEMPT_STATUS,
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

            jdbcTemplate.update(
                    ExperimentSql.UPDATE_JOB_STATUS,
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
        });
    }

    @Override
    public void finalizeAttemptRetryableFailure(
            UUID ownerUserId,
            JobId jobId,
            AttemptId attemptId,
            String failureCode,
            String failureMessage,
            Instant finishTime,
            Instant nextRetryTime
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update(
                    ExperimentSql.UPDATE_ATTEMPT_STATUS,
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

            jdbcTemplate.update(
                    ExperimentSql.UPDATE_JOB_STATUS,
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
        });
    }

    @Override
    public void finalizeAttemptTerminalFailure(
            UUID ownerUserId,
            JobId jobId,
            AttemptId attemptId,
            String failureCode,
            String failureMessage,
            Instant finishTime
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update(
                    ExperimentSql.UPDATE_ATTEMPT_STATUS,
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

            jdbcTemplate.update(
                    ExperimentSql.UPDATE_JOB_STATUS,
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
        });
    }

    @Override
    public void finalizeAttemptCancelled(UUID ownerUserId, JobId jobId, AttemptId attemptId, Instant finishTime) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update(
                    ExperimentSql.UPDATE_ATTEMPT_STATUS,
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

            jdbcTemplate.update(
                    ExperimentSql.UPDATE_JOB_STATUS,
                    JobStatus.CANCELLED.name(),
                    null,
                    toTimestamp(finishTime),
                    null,
                    null,
                    null,
                    toTimestamp(finishTime),
                    jobId.value(),
                    ownerUserId
            );
        });
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

    private static Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }
}

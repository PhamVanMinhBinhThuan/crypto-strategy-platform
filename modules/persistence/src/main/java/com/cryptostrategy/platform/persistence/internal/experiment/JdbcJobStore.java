package com.cryptostrategy.platform.persistence.internal.experiment;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.experiment.api.port.out.JobStore;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JdbcJobStore implements JobStore {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ExperimentRows rows;
    private final ExperimentJsonMapper jsonMapper;

    public JdbcJobStore(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            ExperimentRows rows,
            ExperimentJsonMapper jsonMapper
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate cannot be null");
        this.rows = Objects.requireNonNull(rows, "rows cannot be null");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper cannot be null");
    }

    @Override
    public void insertJob(UUID ownerUserId, Job job, OutboxEvent outboxEvent) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update(
                    ExperimentSql.INSERT_JOB,
                    job.jobId().value(),
                    job.experimentId().value(),
                    job.candidateId() != null ? job.candidateId().value() : null,
                    job.jobType().name(),
                    job.status().name(),
                    job.correlationId(),
                    job.totalWork(),
                    job.completedWork(),
                    job.failedWork(),
                    job.bestScore(),
                    toTimestamp(job.queuedAt()),
                    toTimestamp(job.startedAt()),
                    toTimestamp(job.finishedAt()),
                    toTimestamp(job.nextRetryAt()),
                    job.failureCode(),
                    job.failureMessage(),
                    toTimestamp(job.createdAt()),
                    toTimestamp(job.updatedAt())
            );

            if (outboxEvent != null) {
                jdbcTemplate.update(
                        ExperimentSql.INSERT_OUTBOX_EVENT,
                        outboxEvent.outboxEventId(),
                        outboxEvent.messageId(),
                        outboxEvent.aggregateType(),
                        outboxEvent.aggregateId(),
                        outboxEvent.eventType(),
                        outboxEvent.eventVersion(),
                        outboxEvent.payloadJson(),
                        jsonMapper.writeJson(outboxEvent.headers()),
                        toTimestamp(outboxEvent.occurredAt()),
                        toTimestamp(Instant.now())
                );
            }
        });
    }

    @Override
    public Optional<Job> findJobById(UUID ownerUserId, JobId jobId) {
        try {
            Job job = jdbcTemplate.queryForObject(
                    ExperimentSql.SELECT_JOB_BY_ID,
                    rows::mapJob,
                    jobId.value(),
                    ownerUserId
            );
            return Optional.ofNullable(job);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Job> findBacktestJobByCandidateId(UUID ownerUserId, CandidateId candidateId) {
        try {
            Job job = jdbcTemplate.queryForObject(
                    ExperimentSql.SELECT_BACKTEST_JOB_BY_CANDIDATE,
                    rows::mapJob,
                    candidateId.value(),
                    ownerUserId
            );
            return Optional.ofNullable(job);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Job> listJobsByExperimentId(UUID ownerUserId, ExperimentId experimentId) {
        return jdbcTemplate.query(
                ExperimentSql.SELECT_JOBS_BY_EXPERIMENT_ID,
                rows::mapJob,
                experimentId.value(),
                ownerUserId
        );
    }

    @Override
    public List<Job> listUnfinishedJobs() {
        return jdbcTemplate.query(
                ExperimentSql.SELECT_UNFINISHED_JOBS,
                rows::mapJob
        );
    }

    @Override
    public void updateJobStatus(UUID ownerUserId, JobId jobId, JobStatus newStatus, Instant updatedAt) {
        jdbcTemplate.update(
                ExperimentSql.UPDATE_JOB_STATUS,
                newStatus.name(),
                newStatus == JobStatus.RUNNING ? toTimestamp(updatedAt) : null,
                newStatus.isTerminal() ? toTimestamp(updatedAt) : null,
                null,
                null,
                null,
                toTimestamp(updatedAt),
                jobId.value(),
                ownerUserId
        );
    }

    @Override
    public void cancelJobWithOutbox(UUID ownerUserId, JobId jobId, JobStatus newStatus, OutboxEvent outboxEvent, Instant updatedAt) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update(
                    ExperimentSql.UPDATE_JOB_STATUS,
                    newStatus.name(),
                    null,
                    toTimestamp(updatedAt),
                    null,
                    null,
                    null,
                    toTimestamp(updatedAt),
                    jobId.value(),
                    ownerUserId
            );

            if (outboxEvent != null) {
                jdbcTemplate.update(
                        ExperimentSql.INSERT_OUTBOX_EVENT,
                        outboxEvent.outboxEventId(),
                        outboxEvent.messageId(),
                        outboxEvent.aggregateType(),
                        outboxEvent.aggregateId(),
                        outboxEvent.eventType(),
                        outboxEvent.eventVersion(),
                        outboxEvent.payloadJson(),
                        jsonMapper.writeJson(outboxEvent.headers()),
                        toTimestamp(outboxEvent.occurredAt()),
                        toTimestamp(Instant.now())
                );
            }
        });
    }

    @Override
    public void cancelJobWithoutOutbox(UUID ownerUserId, JobId jobId, JobStatus newStatus, Instant updatedAt) {
        jdbcTemplate.update(
                ExperimentSql.UPDATE_JOB_STATUS,
                newStatus.name(),
                null,
                toTimestamp(updatedAt),
                null,
                null,
                null,
                toTimestamp(updatedAt),
                jobId.value(),
                ownerUserId
        );
    }

    @Override
    public void requeueRetryWithOutbox(UUID ownerUserId, JobId jobId, OutboxEvent outboxEvent, Instant queuedAt) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update(
                    ExperimentSql.UPDATE_JOB_STATUS,
                    JobStatus.QUEUED.name(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    toTimestamp(queuedAt),
                    jobId.value(),
                    ownerUserId
            );

            if (outboxEvent != null) {
                jdbcTemplate.update(
                        ExperimentSql.INSERT_OUTBOX_EVENT,
                        outboxEvent.outboxEventId(),
                        outboxEvent.messageId(),
                        outboxEvent.aggregateType(),
                        outboxEvent.aggregateId(),
                        outboxEvent.eventType(),
                        outboxEvent.eventVersion(),
                        outboxEvent.payloadJson(),
                        jsonMapper.writeJson(outboxEvent.headers()),
                        toTimestamp(outboxEvent.occurredAt()),
                        toTimestamp(Instant.now())
                );
            }
        });
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }
}

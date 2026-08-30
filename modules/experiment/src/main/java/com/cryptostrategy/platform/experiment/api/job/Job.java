package com.cryptostrategy.platform.experiment.api.job;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record Job(
        JobId jobId,
        ExperimentId experimentId,
        CandidateId candidateId,
        JobType jobType,
        JobStatus status,
        String correlationId,
        int totalWork,
        int completedWork,
        int failedWork,
        BigDecimal bestScore,
        Instant queuedAt,
        Instant startedAt,
        Instant finishedAt,
        Instant nextRetryAt,
        String failureCode,
        String failureMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public Job {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(jobType, "jobType cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(correlationId, "correlationId cannot be null");
        if (totalWork <= 0) {
            throw new IllegalArgumentException("totalWork must be positive");
        }
        if (completedWork < 0 || failedWork < 0) {
            throw new IllegalArgumentException("completedWork and failedWork cannot be negative");
        }
        if (completedWork + failedWork > totalWork) {
            throw new IllegalArgumentException("completedWork + failedWork cannot exceed totalWork");
        }
        if (jobType == JobType.SEARCH && candidateId != null) {
            throw new IllegalArgumentException("Search job cannot have a candidateId");
        }
        if (jobType == JobType.BACKTEST && candidateId == null) {
            throw new IllegalArgumentException("Backtest job must have a candidateId");
        }
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    }

    public static Job createBacktestJob(
            JobId jobId,
            ExperimentId experimentId,
            CandidateId candidateId,
            String correlationId,
            Instant createdAt
    ) {
        return new Job(
                jobId,
                experimentId,
                candidateId,
                JobType.BACKTEST,
                JobStatus.QUEUED,
                correlationId,
                1,
                0,
                0,
                null,
                createdAt,
                null,
                null,
                null,
                null,
                null,
                createdAt,
                createdAt
        );
    }

    public static Job createSearchJob(
            JobId jobId,
            ExperimentId experimentId,
            String correlationId,
            int totalGenerations,
            Instant createdAt
    ) {
        return new Job(
                jobId,
                experimentId,
                null,
                JobType.SEARCH,
                JobStatus.QUEUED,
                correlationId,
                totalGenerations,
                0,
                0,
                null,
                createdAt,
                null,
                null,
                null,
                null,
                null,
                createdAt,
                createdAt
        );
    }
}

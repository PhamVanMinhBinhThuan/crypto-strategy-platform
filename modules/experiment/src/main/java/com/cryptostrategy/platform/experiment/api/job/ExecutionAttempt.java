package com.cryptostrategy.platform.experiment.api.job;

import com.cryptostrategy.platform.experiment.api.CandidateId;

import java.time.Instant;
import java.util.Objects;

public record ExecutionAttempt(
        AttemptId attemptId,
        JobId jobId,
        CandidateId candidateId,
        int attemptNo,
        AttemptStatus status,
        String workerId,
        Instant startedAt,
        Instant finishedAt,
        Instant nextRetryAt,
        String failureCode,
        String failureMessage,
        boolean retryable,
        Instant createdAt
) {
    public ExecutionAttempt {
        Objects.requireNonNull(attemptId, "attemptId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(candidateId, "candidateId cannot be null");
        if (attemptNo <= 0) {
            throw new IllegalArgumentException("attemptNo must be positive");
        }
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public static ExecutionAttempt start(
            AttemptId attemptId,
            JobId jobId,
            CandidateId candidateId,
            int attemptNo,
            String workerId,
            Instant startedAt
    ) {
        return new ExecutionAttempt(
                attemptId,
                jobId,
                candidateId,
                attemptNo,
                AttemptStatus.RUNNING,
                workerId,
                startedAt,
                null,
                null,
                null,
                null,
                false,
                startedAt
        );
    }
}

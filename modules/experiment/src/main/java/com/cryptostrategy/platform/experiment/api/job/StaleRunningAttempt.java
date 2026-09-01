package com.cryptostrategy.platform.experiment.api.job;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;

import java.time.Instant;
import java.util.Objects;

public record StaleRunningAttempt(
        JobId jobId,
        AttemptId attemptId,
        ExperimentId experimentId,
        CandidateId candidateId,
        String workerId,
        Instant startedAt
) {
    public StaleRunningAttempt {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(attemptId, "attemptId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(startedAt, "startedAt cannot be null");
    }
}

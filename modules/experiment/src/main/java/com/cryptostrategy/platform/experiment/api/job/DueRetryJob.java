package com.cryptostrategy.platform.experiment.api.job;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;

import java.time.Instant;
import java.util.Objects;

public record DueRetryJob(
        JobId jobId,
        ExperimentId experimentId,
        CandidateId candidateId,
        Instant nextRetryAt
) {
    public DueRetryJob {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(nextRetryAt, "nextRetryAt cannot be null");
    }
}

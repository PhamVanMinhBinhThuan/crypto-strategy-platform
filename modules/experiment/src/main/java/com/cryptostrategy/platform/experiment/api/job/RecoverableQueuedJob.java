package com.cryptostrategy.platform.experiment.api.job;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;

import java.time.Instant;
import java.util.Objects;

public record RecoverableQueuedJob(
        JobId jobId,
        ExperimentId experimentId,
        CandidateId candidateId,
        Instant queuedAt
) {
    public RecoverableQueuedJob {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(queuedAt, "queuedAt cannot be null");
    }
}

package com.cryptostrategy.platform.experiment.api.job;

import com.cryptostrategy.platform.experiment.api.ExperimentId;

import java.time.Instant;
import java.util.Objects;

public record StopCandidateExperiment(
        ExperimentId experimentId,
        Instant stopRequestedAt
) {
    public StopCandidateExperiment {
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
    }
}

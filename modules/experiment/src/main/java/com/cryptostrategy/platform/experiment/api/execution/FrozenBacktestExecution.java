package com.cryptostrategy.platform.experiment.api.execution;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.Job;
import java.util.Objects;

/** Owner-authorized, immutable execution graph resolved from persisted Experiment state. */
public record FrozenBacktestExecution(
        Experiment experiment,
        ExperimentManifest manifest,
        CandidateDefinition candidate,
        Job job,
        ExecutionAttempt attempt
) {
    public FrozenBacktestExecution {
        Objects.requireNonNull(experiment, "experiment cannot be null");
        Objects.requireNonNull(manifest, "manifest cannot be null");
        Objects.requireNonNull(candidate, "candidate cannot be null");
        Objects.requireNonNull(job, "job cannot be null");
        Objects.requireNonNull(attempt, "attempt cannot be null");
    }
}

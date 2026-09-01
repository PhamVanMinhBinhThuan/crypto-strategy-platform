package com.cryptostrategy.platform.backtesting.api.model;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.util.Objects;
import java.util.UUID;

/** Public command contains identity only; executable inputs are resolved from frozen evidence. */
public record BacktestRunCommand(
        UUID ownerUserId,
        ExperimentId experimentId,
        CandidateId candidateId,
        JobId jobId,
        AttemptId attemptId,
        int batchSize
) {
    public BacktestRunCommand {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(candidateId, "candidateId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(attemptId, "attemptId cannot be null");
        if (batchSize < 1 || batchSize > 5_000) {
            throw new IllegalArgumentException("batchSize must be between 1 and 5000");
        }
    }
}

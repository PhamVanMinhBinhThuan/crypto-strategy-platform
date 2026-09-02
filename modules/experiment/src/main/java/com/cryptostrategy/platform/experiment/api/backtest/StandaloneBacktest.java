package com.cryptostrategy.platform.experiment.api.backtest;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.time.Instant;
import java.util.Objects;

/** Durable public resource backed by one immutable single-run Experiment graph. */
public record StandaloneBacktest(
        BacktestId backtestId,
        ExperimentId experimentId,
        CandidateId candidateId,
        JobId jobId,
        Instant createdAt) {
    public StandaloneBacktest {
        Objects.requireNonNull(backtestId, "backtestId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(candidateId, "candidateId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
        if (backtestId.value().equals(experimentId.value())
                || backtestId.value().equals(candidateId.value())
                || backtestId.value().equals(jobId.value())) {
            throw new IllegalArgumentException(
                    "backtestId must be distinct from backing execution identities");
        }
    }
}

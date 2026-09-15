package com.cryptostrategy.platform.backtesting.internal;

import com.cryptostrategy.platform.backtesting.api.model.BacktestAssumptions;
import com.cryptostrategy.platform.backtesting.api.model.BacktestProvenance;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.strategy.api.model.StrategySupplementalInput;
import java.util.Map;
import java.util.Objects;

public record ResolvedBacktestRun(ExperimentId experimentId, CandidateId candidateId, JobId jobId,
        AttemptId attemptId, DatasetSnapshot dataset, BacktestProvenance provenance,
        BacktestAssumptions assumptions, int batchSize, int lookback,
        Map<String, StrategySupplementalInput> supplementalInputs) {
    public ResolvedBacktestRun {
        Objects.requireNonNull(experimentId); Objects.requireNonNull(candidateId); Objects.requireNonNull(jobId);
        Objects.requireNonNull(attemptId); Objects.requireNonNull(dataset); Objects.requireNonNull(provenance);
        Objects.requireNonNull(assumptions);
        supplementalInputs = Map.copyOf(Objects.requireNonNull(supplementalInputs));
        if (batchSize < 1 || batchSize > 5_000 || lookback < 1) throw new IllegalArgumentException("Invalid resolved run bounds");
    }

    public ResolvedBacktestRun(ExperimentId experimentId, CandidateId candidateId, JobId jobId,
            AttemptId attemptId, DatasetSnapshot dataset, BacktestProvenance provenance,
            BacktestAssumptions assumptions, int batchSize, int lookback) {
        this(experimentId, candidateId, jobId, attemptId, dataset, provenance, assumptions,
                batchSize, lookback, Map.of());
    }
}

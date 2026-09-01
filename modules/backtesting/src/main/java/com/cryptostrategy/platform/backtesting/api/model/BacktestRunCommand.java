package com.cryptostrategy.platform.backtesting.api.model;
import com.cryptostrategy.platform.experiment.api.*;
import com.cryptostrategy.platform.experiment.api.job.*;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import java.util.Objects;
public record BacktestRunCommand(ExperimentId experimentId,CandidateId candidateId,JobId jobId,AttemptId attemptId,
        DatasetSnapshot dataset,BacktestProvenance provenance,BacktestAssumptions assumptions,int batchSize,int lookback){
    public BacktestRunCommand{Objects.requireNonNull(experimentId);Objects.requireNonNull(candidateId);Objects.requireNonNull(jobId);Objects.requireNonNull(attemptId);Objects.requireNonNull(dataset);Objects.requireNonNull(provenance);Objects.requireNonNull(assumptions);if(batchSize<1||batchSize>5000||lookback<1)throw new IllegalArgumentException("Invalid run bounds");}
}

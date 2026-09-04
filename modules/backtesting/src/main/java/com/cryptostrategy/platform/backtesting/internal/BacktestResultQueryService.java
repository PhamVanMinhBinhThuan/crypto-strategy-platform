package com.cryptostrategy.platform.backtesting.internal;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.backtesting.api.port.in.GetBacktestResultUseCase;
import com.cryptostrategy.platform.backtesting.api.port.out.BacktestResultReader;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.util.Objects;
import java.util.Optional;

public final class BacktestResultQueryService implements GetBacktestResultUseCase {
    private final BacktestResultReader results;

    public BacktestResultQueryService(BacktestResultReader results) {
        this.results = Objects.requireNonNull(results, "results");
    }

    @Override
    public Optional<BacktestResult> getByResultId(BacktestResultId resultId) {
        return results.findById(Objects.requireNonNull(resultId, "resultId"));
    }

    @Override
    public Optional<BacktestResult> getByJobId(JobId jobId) {
        return results.findByJobId(Objects.requireNonNull(jobId, "jobId"));
    }
}

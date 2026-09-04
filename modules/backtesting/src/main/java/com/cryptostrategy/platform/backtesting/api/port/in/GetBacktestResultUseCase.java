package com.cryptostrategy.platform.backtesting.api.port.in;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.util.Optional;

/** Reads the immutable result associated with a successfully completed Backtest Job. */
public interface GetBacktestResultUseCase {
    Optional<BacktestResult> getByResultId(BacktestResultId resultId);

    Optional<BacktestResult> getByJobId(JobId jobId);
}

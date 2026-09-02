package com.cryptostrategy.platform.backtesting.api.port.out;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.util.Optional;

public interface BacktestResultReader {
    Optional<BacktestResult> findById(BacktestResultId id);

    Optional<BacktestResult> findByJobId(JobId jobId);
}

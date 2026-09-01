package com.cryptostrategy.platform.execution.api.port.in;

import com.cryptostrategy.platform.backtesting.api.PreparedBacktestOutcome;
import com.cryptostrategy.platform.execution.api.BacktestCompletionOutcome;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.JobId;

public interface CompleteBacktestAttemptUseCase {

    /**
     * Atomically coordinates attempt SUCCEEDED finalization, backtest persistence, evaluation calculation,
     * and idempotent progress recording inside a single short database transaction.
     */
    BacktestCompletionOutcome completeAttempt(
            JobId jobId,
            AttemptId attemptId,
            PreparedBacktestOutcome preparedOutcome
    );
}

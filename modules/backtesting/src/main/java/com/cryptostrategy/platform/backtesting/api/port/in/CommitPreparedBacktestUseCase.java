package com.cryptostrategy.platform.backtesting.api.port.in;

import com.cryptostrategy.platform.backtesting.api.PreparedBacktestOutcome;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;

public interface CommitPreparedBacktestUseCase {
    BacktestResult commit(PreparedBacktestOutcome preparedOutcome);
}

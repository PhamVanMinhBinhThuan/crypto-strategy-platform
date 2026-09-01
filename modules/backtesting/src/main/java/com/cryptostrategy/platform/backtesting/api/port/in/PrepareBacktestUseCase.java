package com.cryptostrategy.platform.backtesting.api.port.in;

import com.cryptostrategy.platform.backtesting.api.PreparedBacktestOutcome;
import com.cryptostrategy.platform.backtesting.api.model.BacktestRunCommand;

public interface PrepareBacktestUseCase {
    PreparedBacktestOutcome prepare(BacktestRunCommand command);
}

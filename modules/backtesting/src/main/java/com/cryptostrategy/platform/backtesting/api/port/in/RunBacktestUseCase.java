package com.cryptostrategy.platform.backtesting.api.port.in;
import com.cryptostrategy.platform.backtesting.api.model.*;
public interface RunBacktestUseCase { BacktestResult run(BacktestRunCommand command); }

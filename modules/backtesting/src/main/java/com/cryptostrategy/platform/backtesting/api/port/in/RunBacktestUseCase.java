package com.cryptostrategy.platform.backtesting.api.port.in;
import com.cryptostrategy.platform.backtesting.api.model.*;import com.cryptostrategy.platform.strategy.api.Strategy;
public interface RunBacktestUseCase { BacktestResult run(BacktestRunCommand command, Strategy strategy); }

package com.cryptostrategy.platform.backtesting.api.port.out;
import com.cryptostrategy.platform.backtesting.api.model.BacktestRunCommand;
@FunctionalInterface public interface BacktestLineageVerifier { void verifySuccessful(BacktestRunCommand command); }

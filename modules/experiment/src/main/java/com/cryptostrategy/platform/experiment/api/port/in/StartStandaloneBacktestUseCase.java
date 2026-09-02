package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktestAcceptance;
import com.cryptostrategy.platform.experiment.api.backtest.StartStandaloneBacktestCommand;
import java.util.UUID;

@FunctionalInterface
public interface StartStandaloneBacktestUseCase {
    String OPERATION = "START_BACKTEST";

    StandaloneBacktestAcceptance startStandaloneBacktest(
            UUID ownerUserId, StartStandaloneBacktestCommand command);
}

package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.backtest.BacktestId;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktest;
import java.util.Optional;
import java.util.UUID;

/** Owner-scoped lookup of the public standalone Backtest resource. */
public interface GetStandaloneBacktestUseCase {
    Optional<StandaloneBacktest> getStandaloneBacktest(UUID ownerUserId, BacktestId backtestId);
}

package com.cryptostrategy.platform.backtesting.api;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.Trade;

import java.util.List;
import java.util.Objects;

public record PreparedBacktestOutcome(
        BacktestResult result,
        List<Trade> trades
) {
    public PreparedBacktestOutcome {
        Objects.requireNonNull(result, "result cannot be null");
        trades = trades != null ? List.copyOf(trades) : List.copyOf(result.trades());
    }
}

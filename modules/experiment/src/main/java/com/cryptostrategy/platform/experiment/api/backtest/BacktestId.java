package com.cryptostrategy.platform.experiment.api.backtest;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

/** Public identity of one standalone Backtest request. */
public record BacktestId(String value) implements UlidIdentifier {
    public BacktestId {
        value = Ulids.requireValid(value);
    }

    public static BacktestId generate() {
        return new BacktestId(Ulids.generate());
    }
}

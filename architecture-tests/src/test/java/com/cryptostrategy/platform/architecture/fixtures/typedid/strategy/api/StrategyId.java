package com.cryptostrategy.platform.architecture.fixtures.typedid.strategy.api;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record StrategyId(String value) implements UlidIdentifier {
    public StrategyId {
        value = Ulids.requireValid(value);
    }
}

package com.cryptostrategy.platform.strategy.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record StrategyObservationId(String value) implements UlidIdentifier {
    public StrategyObservationId {
        value = Ulids.requireValid(value);
    }
}

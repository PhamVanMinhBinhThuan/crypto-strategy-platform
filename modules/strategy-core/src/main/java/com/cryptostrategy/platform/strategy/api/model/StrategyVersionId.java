package com.cryptostrategy.platform.strategy.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record StrategyVersionId(String value) implements UlidIdentifier {
    public StrategyVersionId { value = Ulids.requireValid(value); }
    public static StrategyVersionId generate() { return new StrategyVersionId(Ulids.generate()); }
}

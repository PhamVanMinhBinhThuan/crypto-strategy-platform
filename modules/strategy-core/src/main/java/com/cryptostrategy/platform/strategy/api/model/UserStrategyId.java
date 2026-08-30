package com.cryptostrategy.platform.strategy.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record UserStrategyId(String value) implements UlidIdentifier {
    public UserStrategyId { value = Ulids.requireValid(value); }
    public static UserStrategyId generate() { return new UserStrategyId(Ulids.generate()); }
}

package com.cryptostrategy.platform.strategy.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record UserStrategyVersionId(String value) implements UlidIdentifier {
    public UserStrategyVersionId { value = Ulids.requireValid(value); }
    public static UserStrategyVersionId generate() { return new UserStrategyVersionId(Ulids.generate()); }
}

package com.cryptostrategy.platform.strategy.api.model.user;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.util.Objects;
public record UserStrategyComponent(StrategyReference strategyReference, StrategyParameterSet parameters) implements Comparable<UserStrategyComponent> {
    public UserStrategyComponent { Objects.requireNonNull(strategyReference); Objects.requireNonNull(parameters); }
    @Override public int compareTo(UserStrategyComponent other) { return strategyReference.compareTo(other.strategyReference); }
}

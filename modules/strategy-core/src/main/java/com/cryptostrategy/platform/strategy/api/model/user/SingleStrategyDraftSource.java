package com.cryptostrategy.platform.strategy.api.model.user;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.util.Objects;
public record SingleStrategyDraftSource(StrategyReference strategyReference, StrategyParameterSet parameters) implements StrategyDraftSource {
    public SingleStrategyDraftSource { Objects.requireNonNull(strategyReference); Objects.requireNonNull(parameters); }
}

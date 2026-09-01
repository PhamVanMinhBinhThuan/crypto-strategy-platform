package com.cryptostrategy.platform.strategy.api;
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
public interface StrategyPlugin {
    StrategyDescriptor descriptor();
    Strategy create(StrategyParameterSet parameters);
    default int requiredLookback(StrategyParameterSet parameters) { return descriptor().requiredLookback(); }
}

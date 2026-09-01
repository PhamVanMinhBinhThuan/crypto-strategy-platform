package com.cryptostrategy.platform.strategy.api.port.in;

import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.util.List;
import java.util.Map;

public interface StrategyRegistry {
    List<StrategyDescriptor> listAvailable();
    StrategyDescriptor descriptor(StrategyPluginId pluginId, SemanticVersion version);
    StrategyParameterSet resolveParameters(StrategyPluginId pluginId, SemanticVersion version, Map<String, StrategyParameterValue> supplied);
    int requiredLookback(StrategyPluginId pluginId, SemanticVersion version, Map<String, StrategyParameterValue> supplied);
    Strategy create(StrategyPluginId pluginId, SemanticVersion version, Map<String, StrategyParameterValue> supplied);
}

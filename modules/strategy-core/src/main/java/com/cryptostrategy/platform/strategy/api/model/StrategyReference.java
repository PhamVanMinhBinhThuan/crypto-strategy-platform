package com.cryptostrategy.platform.strategy.api.model;

import java.util.Objects;

public record StrategyReference(StrategyVersionId strategyVersionId, StrategyPluginId pluginId,
        SemanticVersion implementationVersion) implements Comparable<StrategyReference> {
    public StrategyReference { Objects.requireNonNull(strategyVersionId); Objects.requireNonNull(pluginId); Objects.requireNonNull(implementationVersion); }
    @Override public int compareTo(StrategyReference other) {
        int result = pluginId.compareTo(other.pluginId);
        if (result == 0) result = implementationVersion.compareTo(other.implementationVersion);
        return result == 0 ? strategyVersionId.value().compareTo(other.strategyVersionId.value()) : result;
    }
}

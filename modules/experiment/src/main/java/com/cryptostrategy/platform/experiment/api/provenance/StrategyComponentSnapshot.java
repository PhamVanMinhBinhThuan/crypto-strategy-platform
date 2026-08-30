package com.cryptostrategy.platform.experiment.api.provenance;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

public record StrategyComponentSnapshot(
        String strategyRefId,
        String version,
        Map<String, Object> parameterOverrides,
        BigDecimal weight,
        int position
) {
    public StrategyComponentSnapshot {
        Objects.requireNonNull(strategyRefId, "strategyRefId cannot be null");
        Objects.requireNonNull(version, "version cannot be null");
        parameterOverrides = parameterOverrides != null ? Map.copyOf(parameterOverrides) : Map.of();
        if (position < 0) {
            throw new IllegalArgumentException("position cannot be negative");
        }
    }
}

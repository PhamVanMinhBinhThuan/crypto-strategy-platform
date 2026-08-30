package com.cryptostrategy.platform.strategy.api.model.parameter;

import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public record StrategyParameterSet(SortedMap<String, StrategyParameterValue> values) {
    public StrategyParameterSet {
        Objects.requireNonNull(values); values = java.util.Collections.unmodifiableSortedMap(new TreeMap<>(values));
    }
    public static StrategyParameterSet of(Map<String, StrategyParameterValue> values) { return new StrategyParameterSet(new TreeMap<>(values)); }
    public static StrategyParameterSet empty() { return of(Map.of()); }
    public StrategyParameterValue require(String name) {
        StrategyParameterValue value = values.get(name);
        if (value == null) throw new IllegalArgumentException("Missing parameter: " + name);
        return value;
    }
}

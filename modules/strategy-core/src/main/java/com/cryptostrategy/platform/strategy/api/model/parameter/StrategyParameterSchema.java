package com.cryptostrategy.platform.strategy.api.model.parameter;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record StrategyParameterSchema(List<ParameterDefinition> definitions,
        List<CrossParameterConstraint> constraints) {
    public StrategyParameterSchema {
        Objects.requireNonNull(definitions); Objects.requireNonNull(constraints);
        definitions = definitions.stream().sorted(Comparator.comparing(ParameterDefinition::name)).toList();
        constraints = List.copyOf(constraints);
        if (definitions.stream().map(ParameterDefinition::name).distinct().count() != definitions.size()) throw new IllegalArgumentException("Duplicate parameter definition");
    }
    public static StrategyParameterSchema empty() { return new StrategyParameterSchema(List.of(), List.of()); }
}

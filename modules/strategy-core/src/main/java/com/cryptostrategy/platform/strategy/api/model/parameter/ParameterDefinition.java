package com.cryptostrategy.platform.strategy.api.model.parameter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public record ParameterDefinition(String name, ParameterType type, boolean required,
        Optional<StrategyParameterValue> defaultValue, Optional<BigDecimal> minimum,
        Optional<BigDecimal> maximum, Set<String> allowedValues, String description) {
    private static final Pattern NAME = Pattern.compile("^[a-z][A-Za-z0-9]*$");
    public ParameterDefinition {
        Objects.requireNonNull(name); Objects.requireNonNull(type); Objects.requireNonNull(defaultValue);
        Objects.requireNonNull(minimum); Objects.requireNonNull(maximum); Objects.requireNonNull(allowedValues);
        Objects.requireNonNull(description); allowedValues = Set.copyOf(allowedValues);
        if (!NAME.matcher(name).matches()) throw new IllegalArgumentException("Invalid parameter name");
        if (defaultValue.isPresent() && defaultValue.get().type() != type) throw new IllegalArgumentException("Default type mismatch");
        if (type == ParameterType.ENUM && allowedValues.isEmpty()) throw new IllegalArgumentException("Enum requires allowed values");
        if (type != ParameterType.ENUM && !allowedValues.isEmpty()) throw new IllegalArgumentException("Allowed values require enum type");
        if (minimum.isPresent() && maximum.isPresent() && minimum.get().compareTo(maximum.get()) > 0) throw new IllegalArgumentException("Invalid range");
    }
}

package com.cryptostrategy.platform.strategy.api.model.parameter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public record ParameterDefinition(String name, ParameterType type, boolean required,
        Optional<StrategyParameterValue> defaultValue, Optional<BigDecimal> minimum,
        Optional<BigDecimal> maximum, Set<String> allowedValues, String description,
        Optional<SearchRangeHint> searchRangeHint) {
    private static final Pattern NAME = Pattern.compile("^[a-z][A-Za-z0-9]*$");
    public ParameterDefinition {
        Objects.requireNonNull(name); Objects.requireNonNull(type); Objects.requireNonNull(defaultValue);
        Objects.requireNonNull(minimum); Objects.requireNonNull(maximum); Objects.requireNonNull(allowedValues);
        Objects.requireNonNull(description); Objects.requireNonNull(searchRangeHint);
        allowedValues = Set.copyOf(allowedValues);
        if (!NAME.matcher(name).matches()) throw new IllegalArgumentException("Invalid parameter name");
        if (defaultValue.isPresent() && defaultValue.get().type() != type) throw new IllegalArgumentException("Default type mismatch");
        if (type == ParameterType.ENUM && allowedValues.isEmpty()) throw new IllegalArgumentException("Enum requires allowed values");
        if (type != ParameterType.ENUM && !allowedValues.isEmpty()) throw new IllegalArgumentException("Allowed values require enum type");
        if (minimum.isPresent() && maximum.isPresent() && minimum.get().compareTo(maximum.get()) > 0) throw new IllegalArgumentException("Invalid range");
        searchRangeHint.ifPresent(hint -> {
            if ((minimum.isPresent() && hint.minimum().compareTo(minimum.get()) < 0)
                    || (maximum.isPresent() && hint.maximum().compareTo(maximum.get()) > 0)) {
                throw new IllegalArgumentException("Search range hint is outside parameter bounds");
            }
        });
    }

    public ParameterDefinition(String name, ParameterType type, boolean required,
            Optional<StrategyParameterValue> defaultValue, Optional<BigDecimal> minimum,
            Optional<BigDecimal> maximum, Set<String> allowedValues, String description) {
        this(name, type, required, defaultValue, minimum, maximum, allowedValues, description,
                Optional.empty());
    }
}

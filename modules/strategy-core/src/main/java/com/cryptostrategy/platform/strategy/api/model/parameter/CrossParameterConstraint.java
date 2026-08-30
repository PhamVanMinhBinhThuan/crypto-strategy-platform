package com.cryptostrategy.platform.strategy.api.model.parameter;

import java.util.Objects;

public record CrossParameterConstraint(String lowerParameter, String upperParameter) {
    public CrossParameterConstraint { Objects.requireNonNull(lowerParameter); Objects.requireNonNull(upperParameter); }
}

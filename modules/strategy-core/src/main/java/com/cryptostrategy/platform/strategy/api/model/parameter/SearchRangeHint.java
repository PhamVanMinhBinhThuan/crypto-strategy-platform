package com.cryptostrategy.platform.strategy.api.model.parameter;

import java.math.BigDecimal;
import java.util.Objects;

/** Recommended search domain; validation bounds remain owned by ParameterDefinition. */
public record SearchRangeHint(BigDecimal minimum, BigDecimal maximum, BigDecimal step) {
    public SearchRangeHint {
        Objects.requireNonNull(minimum);
        Objects.requireNonNull(maximum);
        Objects.requireNonNull(step);
        if (minimum.compareTo(maximum) > 0 || step.signum() <= 0) {
            throw new IllegalArgumentException("Invalid search range hint");
        }
    }
}

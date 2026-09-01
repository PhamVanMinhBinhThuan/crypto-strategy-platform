package com.cryptostrategy.platform.backtesting.api.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Canonical non-negative rate below one, stored at scale ten. */
public record Rate(BigDecimal value) {
    public Rate {
        Objects.requireNonNull(value, "value");
        value = value.setScale(10, RoundingMode.HALF_EVEN);
        if (value.signum() < 0 || value.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Rate must be in [0, 1)");
        }
    }

    public static Rate of(BigDecimal value) { return new Rate(value); }
}

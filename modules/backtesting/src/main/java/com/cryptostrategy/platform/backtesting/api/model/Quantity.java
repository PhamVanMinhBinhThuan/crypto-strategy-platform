package com.cryptostrategy.platform.backtesting.api.model;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
public record Quantity(BigDecimal value) {
    public Quantity { Objects.requireNonNull(value); value = value.setScale(12, RoundingMode.HALF_EVEN); if (value.signum() <= 0) throw new IllegalArgumentException("Quantity must be positive"); }
}

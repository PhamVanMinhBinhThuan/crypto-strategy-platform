package com.cryptostrategy.platform.backtesting.api.model;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
public record Money(BigDecimal value) {
    public static final int SCALE = 12;
    public Money { Objects.requireNonNull(value); value = value.setScale(SCALE, RoundingMode.HALF_EVEN); if (value.signum() < 0) throw new IllegalArgumentException("Money cannot be negative"); }
    public static Money of(BigDecimal value) { return new Money(value); }
    public static Money zero() { return new Money(BigDecimal.ZERO); }
}

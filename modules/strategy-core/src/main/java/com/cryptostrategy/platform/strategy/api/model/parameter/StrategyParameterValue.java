package com.cryptostrategy.platform.strategy.api.model.parameter;

import java.math.BigDecimal;
import java.util.Objects;

public sealed interface StrategyParameterValue permits StrategyParameterValue.IntegerValue,
        StrategyParameterValue.DecimalValue, StrategyParameterValue.BooleanValue,
        StrategyParameterValue.TextValue, StrategyParameterValue.EnumValue {
    ParameterType type();
    String canonicalText();

    record IntegerValue(long value) implements StrategyParameterValue {
        @Override public ParameterType type() { return ParameterType.INTEGER; }
        @Override public String canonicalText() { return Long.toString(value); }
    }
    record DecimalValue(BigDecimal value) implements StrategyParameterValue {
        public DecimalValue { Objects.requireNonNull(value); value = value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros(); }
        @Override public ParameterType type() { return ParameterType.DECIMAL; }
        @Override public String canonicalText() { return value.toPlainString(); }
    }
    record BooleanValue(boolean value) implements StrategyParameterValue {
        @Override public ParameterType type() { return ParameterType.BOOLEAN; }
        @Override public String canonicalText() { return Boolean.toString(value); }
    }
    record TextValue(String value) implements StrategyParameterValue {
        public TextValue { Objects.requireNonNull(value); }
        @Override public ParameterType type() { return ParameterType.TEXT; }
        @Override public String canonicalText() { return value; }
    }
    record EnumValue(String value) implements StrategyParameterValue {
        public EnumValue { Objects.requireNonNull(value); if (value.isBlank()) throw new IllegalArgumentException("Enum value is blank"); }
        @Override public ParameterType type() { return ParameterType.ENUM; }
        @Override public String canonicalText() { return value; }
    }
}

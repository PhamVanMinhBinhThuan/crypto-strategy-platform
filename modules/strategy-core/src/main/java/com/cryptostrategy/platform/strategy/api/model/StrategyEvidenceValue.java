package com.cryptostrategy.platform.strategy.api.model;

import java.math.BigDecimal;
import java.util.Objects;

public sealed interface StrategyEvidenceValue permits StrategyEvidenceValue.DecimalEvidence,
        StrategyEvidenceValue.IntegerEvidence, StrategyEvidenceValue.TextEvidence,
        StrategyEvidenceValue.BooleanEvidence {
    record DecimalEvidence(BigDecimal value) implements StrategyEvidenceValue { public DecimalEvidence { Objects.requireNonNull(value); value = value.stripTrailingZeros(); } }
    record IntegerEvidence(long value) implements StrategyEvidenceValue { }
    record TextEvidence(String value) implements StrategyEvidenceValue { public TextEvidence { Objects.requireNonNull(value); if (value.contains("<")) throw new IllegalArgumentException("Markup is not evidence"); } }
    record BooleanEvidence(boolean value) implements StrategyEvidenceValue { }
}

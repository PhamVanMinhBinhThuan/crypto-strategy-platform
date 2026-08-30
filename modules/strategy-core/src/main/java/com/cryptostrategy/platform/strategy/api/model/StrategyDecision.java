package com.cryptostrategy.platform.strategy.api.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record StrategyDecision(StrategySignal signal, Instant occurredAt, StrategyReference strategyReference,
        String reasonCode, String reason, Map<String, StrategyEvidenceValue> evidence) {
    public StrategyDecision {
        Objects.requireNonNull(signal); Objects.requireNonNull(occurredAt); Objects.requireNonNull(strategyReference);
        Objects.requireNonNull(reasonCode); Objects.requireNonNull(reason); Objects.requireNonNull(evidence);
        if (reasonCode.isBlank() || reason.isBlank()) throw new IllegalArgumentException("Decision reason is blank");
        evidence = Collections.unmodifiableMap(new TreeMap<>(evidence));
    }
}

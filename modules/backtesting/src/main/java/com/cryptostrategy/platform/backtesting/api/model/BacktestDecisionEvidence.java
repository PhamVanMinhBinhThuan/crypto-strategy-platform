package com.cryptostrategy.platform.backtesting.api.model;

import com.cryptostrategy.platform.strategy.api.model.StrategyDecision;
import com.cryptostrategy.platform.strategy.api.model.StrategyEvidenceValue;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable, persistence-safe projection of authoritative Strategy decision evidence. */
public record BacktestDecisionEvidence(
        Instant occurredAt,
        StrategySignal signal,
        String reasonCode,
        Map<String, String> evidence
) {
    public BacktestDecisionEvidence {
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(signal, "signal");
        reasonCode = required(reasonCode, "reasonCode");
        evidence = Map.copyOf(Objects.requireNonNull(evidence, "evidence"));
    }

    public static BacktestDecisionEvidence from(StrategyDecision decision) {
        Objects.requireNonNull(decision, "decision");
        Map<String, String> values = new LinkedHashMap<>();
        decision.evidence().forEach((key, value) -> values.put(key, canonical(value)));
        return new BacktestDecisionEvidence(
                decision.occurredAt(), decision.signal(), decision.reasonCode(), values);
    }

    public boolean containsSentimentEvidence() {
        return evidence.containsKey("sentimentSnapshotId")
                && evidence.containsKey("sentimentEvidenceFingerprint");
    }

    private static String canonical(StrategyEvidenceValue value) {
        if (value instanceof StrategyEvidenceValue.DecimalEvidence decimal) {
            return decimal.value().toPlainString();
        }
        if (value instanceof StrategyEvidenceValue.IntegerEvidence integer) {
            return Long.toString(integer.value());
        }
        if (value instanceof StrategyEvidenceValue.TextEvidence text) return text.value();
        if (value instanceof StrategyEvidenceValue.BooleanEvidence bool) {
            return Boolean.toString(bool.value());
        }
        throw new IllegalArgumentException("Unsupported Strategy evidence value");
    }

    private static String required(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " cannot be blank");
        return normalized;
    }
}

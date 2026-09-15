package com.cryptostrategy.platform.strategy.api.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** One immutable, time-addressable observation supplied to a Strategy evaluation. */
public record StrategyObservation(
        StrategyObservationId identity,
        String sourceReference,
        Instant occurredAt,
        BigDecimal value,
        BigDecimal weight,
        Map<String, String> metadata
) implements Comparable<StrategyObservation> {
    public StrategyObservation {
        Objects.requireNonNull(identity, "identity");
        sourceReference = required(sourceReference, "sourceReference");
        Objects.requireNonNull(occurredAt, "occurredAt");
        value = canonical(Objects.requireNonNull(value, "value"));
        weight = canonical(Objects.requireNonNull(weight, "weight"));
        if (weight.signum() < 0) throw new IllegalArgumentException("Observation weight cannot be negative");
        metadata = Collections.unmodifiableMap(new TreeMap<>(Objects.requireNonNull(metadata, "metadata")));
        if (metadata.entrySet().stream().anyMatch(entry -> entry.getKey() == null
                || entry.getKey().isBlank() || entry.getValue() == null)) {
            throw new IllegalArgumentException("Observation metadata is invalid");
        }
    }

    @Override
    public int compareTo(StrategyObservation other) {
        int byTime = occurredAt.compareTo(other.occurredAt);
        return byTime != 0 ? byTime : identity.value().compareTo(other.identity.value());
    }

    private static BigDecimal canonical(BigDecimal value) {
        return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
    }

    private static String required(String value, String name) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty()) throw new IllegalArgumentException(name + " is blank");
        return value;
    }
}

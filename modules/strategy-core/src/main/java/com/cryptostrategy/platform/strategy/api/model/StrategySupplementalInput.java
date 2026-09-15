package com.cryptostrategy.platform.strategy.api.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Versioned frozen input supplied by an application boundary before Strategy execution. */
public record StrategySupplementalInput(
        String inputType,
        String schemaVersion,
        StrategyInputSnapshotId snapshotId,
        String snapshotFingerprint,
        Map<String, String> metadata,
        List<StrategyObservation> observations
) {
    public StrategySupplementalInput {
        inputType = required(inputType, "inputType");
        schemaVersion = required(schemaVersion, "schemaVersion");
        Objects.requireNonNull(snapshotId, "snapshotId");
        snapshotFingerprint = required(snapshotFingerprint, "snapshotFingerprint");
        metadata = Collections.unmodifiableMap(new TreeMap<>(Objects.requireNonNull(metadata, "metadata")));
        if (metadata.entrySet().stream().anyMatch(entry -> entry.getKey() == null
                || entry.getKey().isBlank() || entry.getValue() == null)) {
            throw new IllegalArgumentException("Supplemental input metadata is invalid");
        }
        observations = Objects.requireNonNull(observations, "observations").stream()
                .map(value -> Objects.requireNonNull(value, "observation"))
                .sorted()
                .toList();
        var identities = new HashSet<StrategyObservationId>();
        if (observations.stream().anyMatch(value -> !identities.add(value.identity()))) {
            throw new IllegalArgumentException("Duplicate observation identity");
        }
    }

    private static String required(String value, String name) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty()) throw new IllegalArgumentException(name + " is blank");
        return value;
    }
}

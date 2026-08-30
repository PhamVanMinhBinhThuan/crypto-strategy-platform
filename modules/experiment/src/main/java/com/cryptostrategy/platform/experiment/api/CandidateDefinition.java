package com.cryptostrategy.platform.experiment.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record CandidateDefinition(
        CandidateId candidateId,
        ExperimentId experimentId,
        int generationIndex,
        Map<String, Object> definition,
        Map<String, Object> generatorState,
        String fingerprint,
        Instant createdAt
) {
    public CandidateDefinition {
        Objects.requireNonNull(candidateId, "candidateId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        if (generationIndex < 0) {
            throw new IllegalArgumentException("generationIndex cannot be negative");
        }
        definition = definition != null ? Map.copyOf(definition) : Map.of();
        generatorState = generatorState != null ? Map.copyOf(generatorState) : null;
        Objects.requireNonNull(fingerprint, "fingerprint cannot be null");
        if (fingerprint.isBlank()) {
            throw new IllegalArgumentException("fingerprint cannot be blank");
        }
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }
}

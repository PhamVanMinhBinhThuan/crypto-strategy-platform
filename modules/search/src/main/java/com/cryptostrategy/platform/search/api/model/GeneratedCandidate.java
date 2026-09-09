package com.cryptostrategy.platform.search.api.model;

import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.util.Objects;
import java.util.Optional;

public record GeneratedCandidate(
        StrategyParameterSet parameters,
        int generationIndex,
        String fingerprint,
        Optional<CompositeGeneratedCandidate> compositeDefinition
) {
    public GeneratedCandidate {
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(compositeDefinition, "compositeDefinition");
        if (generationIndex < 0) {
            throw new IllegalArgumentException("generationIndex must be non-negative");
        }
        if (fingerprint.isBlank()) {
            throw new IllegalArgumentException("fingerprint must not be blank");
        }
    }

    public GeneratedCandidate(StrategyParameterSet parameters, int generationIndex, String fingerprint) {
        this(parameters, generationIndex, fingerprint, Optional.empty());
    }

    public static GeneratedCandidate composite(CompositeGeneratedCandidate definition) {
        return new GeneratedCandidate(StrategyParameterSet.empty(), definition.generationIndex(),
                definition.fingerprint(), Optional.of(definition));
    }
}

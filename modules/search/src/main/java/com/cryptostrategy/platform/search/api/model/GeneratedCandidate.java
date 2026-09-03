package com.cryptostrategy.platform.search.api.model;

import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.util.Objects;

public record GeneratedCandidate(
        StrategyParameterSet parameters,
        int generationIndex,
        String fingerprint
) {
    public GeneratedCandidate {
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(fingerprint, "fingerprint");
        if (generationIndex < 0) {
            throw new IllegalArgumentException("generationIndex must be non-negative");
        }
        if (fingerprint.isBlank()) {
            throw new IllegalArgumentException("fingerprint must not be blank");
        }
    }
}

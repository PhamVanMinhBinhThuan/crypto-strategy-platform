package com.cryptostrategy.platform.search.api.model;

import java.time.Duration;
import java.util.Objects;

public record SearchStopConditions(
        int maximumCandidates,
        Duration maximumDuration,
        Integer maximumWithoutImprovement) {
    public SearchStopConditions(int maximumCandidates, Duration maximumDuration) {
        this(maximumCandidates, maximumDuration, null);
    }

    public SearchStopConditions {
        Objects.requireNonNull(maximumDuration, "maximumDuration");
        if (maximumCandidates < 1) {
            throw new IllegalArgumentException("maximumCandidates must be positive");
        }
        if (maximumDuration.isZero() || maximumDuration.isNegative()) {
            throw new IllegalArgumentException("maximumDuration must be positive");
        }
        if (maximumWithoutImprovement != null && maximumWithoutImprovement < 1) {
            throw new IllegalArgumentException("maximumWithoutImprovement must be positive when present");
        }
    }
}

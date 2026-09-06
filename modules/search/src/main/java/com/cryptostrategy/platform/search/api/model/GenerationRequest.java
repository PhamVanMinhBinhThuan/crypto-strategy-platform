package com.cryptostrategy.platform.search.api.model;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public record GenerationRequest(
        SearchSpace searchSpace,
        Optional<CompositeSearchSpace> compositeSearchSpace,
        long seed,
        Optional<GeneratorState> priorState,
        int expectedGenerationIndex,
        Set<String> acceptedCandidateFingerprints,
        int remainingDrawBudget
) {
    public GenerationRequest {
        Objects.requireNonNull(searchSpace, "searchSpace");
        Objects.requireNonNull(compositeSearchSpace, "compositeSearchSpace");
        Objects.requireNonNull(priorState, "priorState");
        Objects.requireNonNull(acceptedCandidateFingerprints, "acceptedCandidateFingerprints");
        if (expectedGenerationIndex < 0) {
            throw new IllegalArgumentException("expectedGenerationIndex must be non-negative");
        }
        if (remainingDrawBudget < 1) {
            throw new IllegalArgumentException("remainingDrawBudget must be positive");
        }
        TreeSet<String> canonical = new TreeSet<>();
        for (String fingerprint : acceptedCandidateFingerprints) {
            Objects.requireNonNull(fingerprint, "accepted candidate fingerprint");
            if (fingerprint.isBlank()) {
                throw new IllegalArgumentException("accepted candidate fingerprint must not be blank");
            }
            canonical.add(fingerprint);
        }
        acceptedCandidateFingerprints = Collections.unmodifiableSet(canonical);
    }

    public GenerationRequest(SearchSpace searchSpace, long seed, Optional<GeneratorState> priorState,
            int expectedGenerationIndex, Set<String> acceptedCandidateFingerprints,
            int remainingDrawBudget) {
        this(searchSpace, Optional.empty(), seed, priorState, expectedGenerationIndex,
                acceptedCandidateFingerprints, remainingDrawBudget);
    }

    public static GenerationRequest initial(SearchSpace searchSpace, long seed, int remainingDrawBudget) {
        return new GenerationRequest(searchSpace, Optional.empty(), seed, Optional.empty(), 0, Set.of(),
                remainingDrawBudget);
    }

    public static GenerationRequest composite(CompositeSearchSpace searchSpace, long seed,
            Optional<GeneratorState> priorState, int expectedGenerationIndex,
            Set<String> acceptedCandidateFingerprints, int remainingDrawBudget) {
        return new GenerationRequest(new SearchSpace(java.util.Map.of()), Optional.of(searchSpace), seed,
                priorState, expectedGenerationIndex, acceptedCandidateFingerprints, remainingDrawBudget);
    }
}

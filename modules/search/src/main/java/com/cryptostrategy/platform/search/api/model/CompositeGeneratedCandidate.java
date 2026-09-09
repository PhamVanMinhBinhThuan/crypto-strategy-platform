package com.cryptostrategy.platform.search.api.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Complete semantic candidate definition; runtime/job identities are deliberately excluded. */
public record CompositeGeneratedCandidate(
        List<CompositeCandidateComponent> components,
        SearchCombinationPolicy combinationPolicy,
        int generationIndex,
        String fingerprint) {
    public CompositeGeneratedCandidate {
        Objects.requireNonNull(components, "components");
        Objects.requireNonNull(combinationPolicy, "combinationPolicy");
        if (generationIndex < 0) throw new IllegalArgumentException("generationIndex must be non-negative");
        if (fingerprint == null || fingerprint.isBlank()) {
            throw new IllegalArgumentException("fingerprint must not be blank");
        }
        ArrayList<CompositeCandidateComponent> canonical = new ArrayList<>(components);
        canonical.sort(CompositeCandidateComponent::compareTo);
        if (canonical.isEmpty()
                || new HashSet<>(canonical.stream().map(CompositeCandidateComponent::strategy).toList()).size()
                != canonical.size()) {
            throw new IllegalArgumentException("Candidate components must be non-empty and unique");
        }
        components = List.copyOf(canonical);
    }
}

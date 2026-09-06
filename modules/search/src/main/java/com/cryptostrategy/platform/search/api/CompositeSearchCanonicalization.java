package com.cryptostrategy.platform.search.api;

import com.cryptostrategy.platform.search.api.model.CompositeCandidateComponent;
import com.cryptostrategy.platform.search.api.model.CompositeSearchSpace;
import com.cryptostrategy.platform.search.api.model.SearchCombinationPolicy;
import com.cryptostrategy.platform.search.internal.CanonicalCompositeSearchSpace;
import java.util.List;

/** Public Search-owned canonicalization boundary for orchestration consumers. */
public final class CompositeSearchCanonicalization {
    private CompositeSearchCanonicalization() {}

    public static String searchSpaceFingerprint(CompositeSearchSpace space) {
        return CanonicalCompositeSearchSpace.fingerprint(space);
    }

    public static String candidateFingerprint(
            List<CompositeCandidateComponent> components, SearchCombinationPolicy policy) {
        return CanonicalCompositeSearchSpace.candidateFingerprint(components, policy);
    }
}

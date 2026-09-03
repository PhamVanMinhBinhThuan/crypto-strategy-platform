package com.cryptostrategy.platform.execution.api.port.out;

import com.cryptostrategy.platform.search.api.model.SearchSpace;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Đọc input đã đóng băng và progress authoritative cần để đề xuất allocation ngoài transaction. */
public interface SearchAllocationContextGateway {
    Optional<Context> load(String experimentId, String searchJobId);

    record Context(
            UUID ownerUserId,
            SearchSpace searchSpace,
            Set<String> acceptedCandidateFingerprints,
            int completedWork,
            int failedWork) {
        public Context {
            Objects.requireNonNull(ownerUserId, "ownerUserId");
            Objects.requireNonNull(searchSpace, "searchSpace");
            acceptedCandidateFingerprints = Set.copyOf(acceptedCandidateFingerprints);
            if (completedWork < 0 || failedWork < 0) {
                throw new IllegalArgumentException("progress must be non-negative");
            }
        }
    }
}

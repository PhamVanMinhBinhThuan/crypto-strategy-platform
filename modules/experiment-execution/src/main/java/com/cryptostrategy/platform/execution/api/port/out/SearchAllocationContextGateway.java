package com.cryptostrategy.platform.execution.api.port.out;

import com.cryptostrategy.platform.search.api.model.SearchSpace;
import com.cryptostrategy.platform.search.api.model.CompositeSearchSpace;
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
            Optional<CompositeSearchSpace> compositeSearchSpace,
            Set<String> acceptedCandidateFingerprints,
            int allocatedWork,
            int completedWork,
            int failedWork) {
        public Context {
            Objects.requireNonNull(ownerUserId, "ownerUserId");
            Objects.requireNonNull(searchSpace, "searchSpace");
            Objects.requireNonNull(compositeSearchSpace, "compositeSearchSpace");
            acceptedCandidateFingerprints = Set.copyOf(acceptedCandidateFingerprints);
            if (allocatedWork < 0 || completedWork < 0 || failedWork < 0) {
                throw new IllegalArgumentException("progress must be non-negative");
            }
            if (completedWork + failedWork > allocatedWork) {
                throw new IllegalArgumentException("terminal work cannot exceed allocated work");
            }
        }

        public Context(UUID ownerUserId, SearchSpace searchSpace,
                Set<String> acceptedCandidateFingerprints, int completedWork, int failedWork) {
            this(ownerUserId, searchSpace, Optional.empty(), acceptedCandidateFingerprints,
                    acceptedCandidateFingerprints.size(), completedWork, failedWork);
        }
    }
}

package com.cryptostrategy.platform.execution.api.port.out;

import java.util.UUID;

import com.cryptostrategy.platform.search.api.model.SearchRun;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Composite durable boundary cho Search Run và SEARCH Job authoritative progress. */
public interface TrustedSearchCoordinationGateway {
    Optional<AuthoritativeSnapshot> load(String experimentId);

    /** Kiểm tra lineage Candidate/Backtest/Evaluation; empty nếu trigger không thuộc graph. */
    Optional<AuthoritativeSnapshot> loadCompletion(
            String experimentId, String candidateId, String backtestJobId);

    boolean commit(Transition transition);

    record AuthoritativeSnapshot(
            SearchRun run,
            int allocatedWork,
            int completedWork,
            int failedWork,
            Instant latestAuthoritativeCompletedAt) {
        public AuthoritativeSnapshot {
            Objects.requireNonNull(run, "run");
            if (allocatedWork < 0 || completedWork < 0 || failedWork < 0
                    || completedWork + failedWork > allocatedWork) {
                throw new IllegalArgumentException("authoritative counters are inconsistent");
            }
        }

        public int activeWork() {
            return allocatedWork - completedWork - failedWork;
        }
    }

    record Transition(
            long expectedVersion,
            SearchRun replacement,
            int completedWork,
            int failedWork,
            String processedMessageRef) {
        public Transition {
            if (expectedVersion < 0) throw new IllegalArgumentException("expectedVersion must be non-negative");
            Objects.requireNonNull(replacement, "replacement");
            if (completedWork < 0 || failedWork < 0) throw new IllegalArgumentException("progress must be non-negative");
        }
    }
}

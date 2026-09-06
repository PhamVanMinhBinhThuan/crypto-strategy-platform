package com.cryptostrategy.platform.execution.api.port.out;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Composite durable boundary cho Search Run và SEARCH Job authoritative progress. */
public interface TrustedSearchCoordinationGateway {
    Optional<AuthoritativeSnapshot> load(ExperimentId experimentId);

    /** Kiểm tra lineage Candidate/Backtest/Evaluation; empty nếu trigger không thuộc graph. */
    Optional<AuthoritativeSnapshot> loadCompletion(
            ExperimentId experimentId, CandidateId candidateId, JobId backtestJobId);

    boolean commit(Transition transition);

    record AuthoritativeSnapshot(
            SearchRun run,
            int allocatedWork,
            int completedWork,
            int failedWork,
            Instant latestAuthoritativeCompletedAt,
            int configuredMaximumCandidates,
            int consecutiveWithoutImprovement) {
        public AuthoritativeSnapshot(SearchRun run, int allocatedWork, int completedWork,
                int failedWork, Instant latestAuthoritativeCompletedAt) {
            this(run, allocatedWork, completedWork, failedWork, latestAuthoritativeCompletedAt,
                    run.stopConditions().maximumCandidates(), 0);
        }

        public AuthoritativeSnapshot {
            Objects.requireNonNull(run, "run");
            if (allocatedWork < 0 || completedWork < 0 || failedWork < 0
                    || completedWork + failedWork > allocatedWork) {
                throw new IllegalArgumentException("authoritative counters are inconsistent");
            }
            if (configuredMaximumCandidates < run.stopConditions().maximumCandidates()
                    || consecutiveWithoutImprovement < 0) {
                throw new IllegalArgumentException("authoritative Search bounds are inconsistent");
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
            String messageId) {
        public Transition {
            if (expectedVersion < 0) throw new IllegalArgumentException("expectedVersion must be non-negative");
            Objects.requireNonNull(replacement, "replacement");
            if (completedWork < 0 || failedWork < 0) throw new IllegalArgumentException("progress must be non-negative");
        }
    }
}

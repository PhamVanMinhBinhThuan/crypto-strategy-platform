package com.cryptostrategy.platform.execution.api.port.out;

import com.cryptostrategy.platform.search.api.model.ReproductionVerificationId;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Durable claim/fence store cho async reproduction verification. */
public interface SearchReproductionVerificationGateway {
    List<ExperimentId> findReady(int limit);
    Optional<Work> claimReady(ExperimentId reproductionExperimentId, Instant now);
    boolean complete(Completion completion);

    record Work(ReproductionVerificationId verificationId, long version, UUID ownerUserId,
            ExperimentId sourceExperimentId, ExperimentId reproductionExperimentId) {}

    record Completion(ReproductionVerificationId verificationId, long expectedVersion, String status,
            boolean tradesMatched, boolean metricsMatched, boolean fingerprintsMatched,
            String sourceFingerprint, String reproductionFingerprint,
            Map<String, Object> safeDifferences, Instant finishedAt,
            String failureCode, String failureMessage) {
        public Completion { safeDifferences = Map.copyOf(safeDifferences); }
    }
}

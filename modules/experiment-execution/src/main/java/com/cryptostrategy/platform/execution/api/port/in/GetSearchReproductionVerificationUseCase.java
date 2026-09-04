package com.cryptostrategy.platform.execution.api.port.in;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.execution.api.ReproductionVerificationId;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Owner-scoped read boundary for the durable asynchronous reproduction verdict. */
public interface GetSearchReproductionVerificationUseCase {
    Optional<Snapshot> get(UUID ownerUserId, ExperimentId reproductionExperimentId);

    record Snapshot(
            ReproductionVerificationId verificationId,
            ExperimentId sourceExperimentId,
            ExperimentId reproductionExperimentId,
            String status,
            Boolean tradesMatched,
            Boolean metricsMatched,
            Boolean fingerprintsMatched,
            String sourceEvidenceFingerprint,
            String reproductionEvidenceFingerprint,
            Map<String, Object> differences,
            String failureCode,
            String failureMessage,
            Instant startedAt,
            Instant finishedAt,
            Instant updatedAt) {
        public Snapshot {
            differences = differences == null ? Map.of() : Map.copyOf(differences);
        }
    }
}

package com.cryptostrategy.platform.execution.api.port.in;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Public application command: chỉ tạo durable reproduction work và trả về ngay sau commit. */
public interface StartSearchReproductionUseCase {
    Acceptance start(Command command);

    record Command(UUID ownerUserId, ExperimentId sourceExperimentId, String name,
            String idempotencyKey, String canonicalRequestHash, String correlationId,
            Instant requestedAt, Instant receiptExpiresAt) {
        public Command {
            Objects.requireNonNull(ownerUserId, "ownerUserId");
            Objects.requireNonNull(sourceExperimentId, "sourceExperimentId");
            name = requireText(name, "name");
            idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
            canonicalRequestHash = requireText(canonicalRequestHash, "canonicalRequestHash");
            correlationId = requireText(correlationId, "correlationId");
            Objects.requireNonNull(requestedAt, "requestedAt");
            Objects.requireNonNull(receiptExpiresAt, "receiptExpiresAt");
        }
    }

    record Acceptance(ExperimentId experimentId, JobId searchJobId, String status, boolean replay) {
        public Acceptance {
            Objects.requireNonNull(experimentId, "experimentId");
            Objects.requireNonNull(searchJobId, "searchJobId");
            if (!"QUEUED".equals(status)) throw new IllegalArgumentException("status must be QUEUED");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}

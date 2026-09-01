package com.cryptostrategy.platform.experiment.api.idempotency;

import java.time.Instant;
import java.util.Objects;

public record IdempotencyOutcome(
        String outcomeCode,
        String responseBody,
        Instant completedAt
) {
    public IdempotencyOutcome {
        Objects.requireNonNull(outcomeCode, "outcomeCode cannot be null");
        Objects.requireNonNull(completedAt, "completedAt cannot be null");
    }
}

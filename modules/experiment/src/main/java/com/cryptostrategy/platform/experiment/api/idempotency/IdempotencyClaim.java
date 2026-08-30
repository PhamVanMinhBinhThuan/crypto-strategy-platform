package com.cryptostrategy.platform.experiment.api.idempotency;

import java.util.Objects;
import java.util.Optional;

public record IdempotencyClaim(
        IdempotencyClaimStatus status,
        IdempotencyOutcome existingOutcome
) {
    public IdempotencyClaim {
        Objects.requireNonNull(status, "status cannot be null");
    }

    public static IdempotencyClaim acquired() {
        return new IdempotencyClaim(IdempotencyClaimStatus.ACQUIRED, null);
    }

    public static IdempotencyClaim inProgressReplay() {
        return new IdempotencyClaim(IdempotencyClaimStatus.IN_PROGRESS_REPLAY, null);
    }

    public static IdempotencyClaim completedReplay(IdempotencyOutcome outcome) {
        return new IdempotencyClaim(IdempotencyClaimStatus.COMPLETED_REPLAY, outcome);
    }

    public static IdempotencyClaim conflict() {
        return new IdempotencyClaim(IdempotencyClaimStatus.CONFLICT, null);
    }

    public Optional<IdempotencyOutcome> outcome() {
        return Optional.ofNullable(existingOutcome);
    }
}

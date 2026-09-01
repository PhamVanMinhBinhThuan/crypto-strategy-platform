package com.cryptostrategy.platform.experiment.api.idempotency;

public enum IdempotencyClaimStatus {
    ACQUIRED,
    IN_PROGRESS_REPLAY,
    COMPLETED_REPLAY,
    CONFLICT
}

package com.cryptostrategy.platform.experiment.api.port.out;

import com.cryptostrategy.platform.experiment.api.idempotency.IdempotencyClaim;
import com.cryptostrategy.platform.experiment.api.idempotency.IdempotencyOutcome;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyStore {
    IdempotencyClaim claim(UUID ownerUserId, String scope, String idempotencyKey, String requestHash, Instant expiresAt);
    void complete(UUID ownerUserId, String scope, String idempotencyKey, String outcomeCode, String responseBody);
    Optional<IdempotencyOutcome> getOutcome(UUID ownerUserId, String scope, String idempotencyKey);
}

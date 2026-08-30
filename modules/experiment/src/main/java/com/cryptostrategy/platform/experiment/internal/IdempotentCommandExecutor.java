package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.experiment.api.idempotency.IdempotencyClaim;
import com.cryptostrategy.platform.experiment.api.port.out.IdempotencyStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class IdempotentCommandExecutor {

    private final IdempotencyStore idempotencyStore;

    public IdempotentCommandExecutor(IdempotencyStore idempotencyStore) {
        this.idempotencyStore = Objects.requireNonNull(idempotencyStore, "idempotencyStore cannot be null");
    }

    public <T> T execute(
            UUID ownerUserId,
            String scope,
            String idempotencyKey,
            String requestHash,
            Duration ttl,
            Supplier<T> command,
            Function<T, String> responseSerializer,
            Function<String, T> responseDeserializer
    ) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(scope, "scope cannot be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey cannot be null");
        Objects.requireNonNull(requestHash, "requestHash cannot be null");
        Objects.requireNonNull(command, "command cannot be null");

        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl != null ? ttl : Duration.ofHours(24));

        IdempotencyClaim claim = idempotencyStore.claim(ownerUserId, scope, idempotencyKey, requestHash, expiresAt);

        switch (claim.status()) {
            case CONFLICT -> throw new IdempotencyConflictException(
                    "Idempotency conflict: key '" + idempotencyKey + "' was already used with a different request payload"
            );
            case IN_PROGRESS_REPLAY -> throw new IllegalStateException(
                    "A request with idempotency key '" + idempotencyKey + "' is already in progress"
            );
            case COMPLETED_REPLAY -> {
                String serialized = claim.outcome()
                        .orElseThrow(() -> new IllegalStateException("Completed claim missing outcome"))
                        .responseBody();
                return responseDeserializer.apply(serialized);
            }
            case ACQUIRED -> {
                T result = command.get();
                String serialized = responseSerializer.apply(result);
                idempotencyStore.complete(ownerUserId, scope, idempotencyKey, "200", serialized);
                return result;
            }
            default -> throw new IllegalStateException("Unknown claim status: " + claim.status());
        }
    }
}

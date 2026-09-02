package com.cryptostrategy.platform.api.idempotency;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

/** Prepares the canonical receipt identity; the owner capability commits it atomically. */
@Component
public final class IdempotencyCommandExecutor {
    private static final int MAX_KEY_LENGTH = 255;
    private final IdempotencyService idempotency;

    public IdempotencyCommandExecutor(IdempotencyService idempotency) {
        this.idempotency = Objects.requireNonNull(idempotency, "idempotency");
    }

    public <T> T execute(
            UUID ownerUserId,
            String operation,
            String idempotencyKey,
            Object request,
            BiFunction<String, String, T> command) {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(command, "command");
        String key = requireKey(idempotencyKey);
        String requestHash = idempotency.canonicalRequestHash(
                ownerUserId, operation, Objects.requireNonNull(request, "request"));
        return command.apply(key, requestHash);
    }

    private static String requireKey(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must contain between 1 and 255 characters");
        }
        return key;
    }
}

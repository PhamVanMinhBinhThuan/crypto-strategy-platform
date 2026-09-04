package com.cryptostrategy.platform.api.idempotency;

import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.experiment.api.idempotency.IdempotencyClaim;
import com.cryptostrategy.platform.experiment.api.idempotency.IdempotencyOutcome;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

/** Prepares the canonical receipt identity and executes the command if acquired. */
@Component
public class IdempotencyCommandExecutor {
    private static final int MAX_KEY_LENGTH = 255;
    private final IdempotencyService idempotency;
    private final ObjectMapper json;

    public IdempotencyCommandExecutor(IdempotencyService idempotency, ObjectMapper json) {
        this.idempotency = Objects.requireNonNull(idempotency, "idempotency");
        this.json = Objects.requireNonNull(json, "json");
    }

    public <T> T execute(
            UUID ownerUserId,
            String operation,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            BiFunction<String, String, T> command) {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(command, "command");
        String key = requireKey(idempotencyKey);

        IdempotencyClaim claim = idempotency.claim(ownerUserId, operation, key, request);

        switch (claim.status()) {
            case ACQUIRED -> {
                String requestHash = idempotency.canonicalRequestHash(ownerUserId, operation, request);
                T result = command.apply(key, requestHash);
                try {
                    idempotency.complete(ownerUserId, operation, key, "202", json.writeValueAsString(result));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize response", e);
                }
                return result;
            }
            case IN_PROGRESS_REPLAY -> {
                throw new com.cryptostrategy.platform.api.error.DependencyUnavailableException(
                        "Idempotent command outcome");
            }
            case COMPLETED_REPLAY -> {
                IdempotencyOutcome outcome = claim.outcome().orElseThrow();
                try {
                    return json.readValue(outcome.responseBody(), responseType);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to deserialize outcome", e);
                }
            }
            case CONFLICT -> {
                throw new IdempotencyConflictException("Idempotency conflict: payload does not match");
            }
            default -> throw new IllegalStateException("Unknown claim status");
        }
    }

    public <T> T execute(
            UUID ownerUserId,
            String operation,
            String idempotencyKey,
            Object request,
            BiFunction<String, String, T> command) {
        // Fallback for Backtest that does its own atomic store logic
        String key = requireKey(idempotencyKey);
        String requestHash = idempotency.canonicalRequestHash(ownerUserId, operation, request);
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

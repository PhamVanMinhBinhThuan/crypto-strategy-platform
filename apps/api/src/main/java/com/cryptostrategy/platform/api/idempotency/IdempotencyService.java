package com.cryptostrategy.platform.api.idempotency;

import com.cryptostrategy.platform.experiment.api.idempotency.IdempotencyClaim;
import com.cryptostrategy.platform.experiment.api.idempotency.IdempotencyOutcome;
import com.cryptostrategy.platform.experiment.api.port.out.IdempotencyStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Canonicalizes public commands and delegates durable claims to the F-005 port. */
@Service
public final class IdempotencyService {
    private static final String HASH_VERSION = "idempotency-request-v1";
    private static final int MAX_KEY_LENGTH = 255;

    private final ObjectMapper json;
    private final IdempotencyStore store;
    private final Duration receiptLifetime;
    private final Clock clock;

    @Autowired
    public IdempotencyService(
            ObjectMapper objectMapper,
            IdempotencyStore store,
            @Value("${platform.idempotency.receipt-lifetime:PT24H}") Duration receiptLifetime) {
        this(objectMapper, store, receiptLifetime, Clock.systemUTC());
    }

    IdempotencyService(
            ObjectMapper objectMapper,
            IdempotencyStore store,
            Duration receiptLifetime,
            Clock clock) {
        this.json = Objects.requireNonNull(objectMapper, "objectMapper")
                .copy();
        this.store = Objects.requireNonNull(store, "store");
        this.receiptLifetime = requirePositive(receiptLifetime);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public String canonicalRequestHash(
            UUID authenticatedUserId,
            String operation,
            Object request) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId");
        String normalizedOperation = requireOperation(operation);
        Objects.requireNonNull(request, "request");

        ObjectNode envelope = json.createObjectNode();
        envelope.put("hashVersion", HASH_VERSION);
        envelope.put("operation", normalizedOperation);
        envelope.put("ownerUserId", authenticatedUserId.toString());
        envelope.set("request", canonicalize(json.valueToTree(request)));

        try {
            byte[] canonicalJson = json.writeValueAsBytes(canonicalize(envelope));
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonicalJson);
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Request payload cannot be canonicalized", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public IdempotencyClaim claim(
            UUID authenticatedUserId,
            String operation,
            String idempotencyKey,
            Object request) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId");
        String normalizedOperation = requireOperation(operation);
        String validatedKey = requireKey(idempotencyKey);
        String requestHash = canonicalRequestHash(authenticatedUserId, normalizedOperation, request);
        Instant expiresAt = clock.instant().plus(receiptLifetime);
        return store.claim(
                authenticatedUserId,
                normalizedOperation,
                validatedKey,
                requestHash,
                expiresAt);
    }

    public void complete(
            UUID authenticatedUserId,
            String operation,
            String idempotencyKey,
            String outcomeCode,
            String responseBody) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId");
        store.complete(
                authenticatedUserId,
                requireOperation(operation),
                requireKey(idempotencyKey),
                Objects.requireNonNull(outcomeCode, "outcomeCode"),
                Objects.requireNonNull(responseBody, "responseBody"));
    }

    public Optional<IdempotencyOutcome> getOutcome(
            UUID authenticatedUserId,
            String operation,
            String idempotencyKey) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId");
        return store.getOutcome(
                authenticatedUserId,
                requireOperation(operation),
                requireKey(idempotencyKey));
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode canonical = json.createObjectNode();
            TreeMap<String, JsonNode> fields = new TreeMap<>();
            node.properties().forEach(entry -> fields.put(entry.getKey(), entry.getValue()));
            fields.forEach((name, value) -> canonical.set(name, canonicalize(value)));
            return canonical;
        }
        if (node.isArray()) {
            ArrayNode canonical = json.createArrayNode();
            node.forEach(value -> canonical.add(canonicalize(value)));
            return canonical;
        }
        return node;
    }

    private static String requireOperation(String operation) {
        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("Idempotency operation is required");
        }
        return operation.trim();
    }

    private static String requireKey(String idempotencyKey) {
        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must contain between 1 and 255 characters");
        }
        return idempotencyKey;
    }

    private static Duration requirePositive(Duration duration) {
        Objects.requireNonNull(duration, "receiptLifetime");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Idempotency receipt lifetime must be positive");
        }
        return duration;
    }
}

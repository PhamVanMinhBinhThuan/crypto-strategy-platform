package com.cryptostrategy.platform.contracts.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageEnvelope<T>(
        @JsonProperty("messageId") String messageId,
        @JsonProperty("messageVersion") int messageVersion,
        @JsonProperty("messageType") String messageType,
        @JsonProperty("occurredAt") Instant occurredAt,
        @JsonProperty("correlationId") String correlationId,
        @JsonProperty("payload") T payload
) {
    private static final Pattern ULID_PATTERN = Pattern.compile("^[0-9A-HJKMNP-TV-Z]{26}$");
    private static final Pattern MESSAGE_TYPE_PATTERN = Pattern.compile("^[A-Z0-9_]+$");

    public MessageEnvelope {
        Objects.requireNonNull(messageId, "messageId cannot be null");
        Objects.requireNonNull(messageType, "messageType cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        Objects.requireNonNull(correlationId, "correlationId cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
        if (messageVersion < 1) {
            throw new IllegalArgumentException("messageVersion must be >= 1");
        }
        if (!ULID_PATTERN.matcher(messageId).matches()) {
            throw new IllegalArgumentException("messageId must be a valid 26-character Crockford Base32 ULID: " + messageId);
        }
        if (!MESSAGE_TYPE_PATTERN.matcher(messageType).matches()) {
            throw new IllegalArgumentException("messageType must be UPPER_SNAKE_CASE: " + messageType);
        }
    }
}

package com.cryptostrategy.platform.experiment.api.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record OutboxEvent(
        String outboxEventId,
        String messageId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String eventVersion,
        String payloadJson,
        Map<String, String> headers,
        Instant occurredAt
) {
    public OutboxEvent {
        Objects.requireNonNull(outboxEventId, "outboxEventId cannot be null");
        Objects.requireNonNull(messageId, "messageId cannot be null");
        Objects.requireNonNull(aggregateType, "aggregateType cannot be null");
        Objects.requireNonNull(aggregateId, "aggregateId cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        Objects.requireNonNull(eventVersion, "eventVersion cannot be null");
        Objects.requireNonNull(payloadJson, "payloadJson cannot be null");
        headers = headers != null ? Map.copyOf(headers) : Map.of();
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}

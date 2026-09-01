package com.cryptostrategy.platform.persistence.api.worker;

import java.time.Instant;
import java.util.Objects;

public record OutboxRecord(
        String outboxEventId,
        String messageId,
        String aggregateType,
        String aggregateId,
        String eventType,
        int eventVersion,
        String payload,
        String headers,
        Instant publishedAt,
        int publishAttempts,
        String lastError,
        Instant occurredAt,
        Instant createdAt
) {
    public OutboxRecord {
        Objects.requireNonNull(outboxEventId, "outboxEventId cannot be null");
        Objects.requireNonNull(messageId, "messageId cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
    }
}

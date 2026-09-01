package com.cryptostrategy.platform.worker.consumer;

import com.cryptostrategy.platform.persistence.api.worker.ProcessedMessageStore;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Component
public class DualLayerIdempotencyGuard {

    private final ProcessedMessageStore processedMessageStore;

    public DualLayerIdempotencyGuard(ProcessedMessageStore processedMessageStore) {
        this.processedMessageStore = Objects.requireNonNull(processedMessageStore, "processedMessageStore cannot be null");
    }

    public boolean isAlreadyProcessed(String consumerName, String messageId) {
        return processedMessageStore.isProcessed(consumerName, messageId);
    }

    public boolean markProcessed(String consumerName, String messageId, Duration retention) {
        Instant now = Instant.now();
        Instant expiresAt = retention != null ? now.plus(retention) : now.plus(Duration.ofDays(7));
        return processedMessageStore.insertIfAbsent(consumerName, messageId, now, expiresAt);
    }
}

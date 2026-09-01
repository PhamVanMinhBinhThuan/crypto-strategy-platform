package com.cryptostrategy.platform.persistence.api.worker;

import java.time.Instant;

public interface ProcessedMessageStore {
    boolean isProcessed(String consumerName, String messageId);
    boolean insertIfAbsent(String consumerName, String messageId, Instant processedAt, Instant expiresAt);
    int purgeExpired(Instant threshold);
}

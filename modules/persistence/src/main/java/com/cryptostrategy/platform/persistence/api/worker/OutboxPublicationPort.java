package com.cryptostrategy.platform.persistence.api.worker;

import java.time.Instant;
import java.util.List;

public interface OutboxPublicationPort {
    List<OutboxRecord> listUnpublishedBatch(int limit);
    void recordPublishSuccess(String outboxEventId, Instant publishedAt);
    void recordPublishFailure(String outboxEventId, String lastError, Instant attemptedAt);
    void markSuppressed(String outboxEventId, String reason);
}

package com.cryptostrategy.platform.worker.engine;

import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.persistence.api.worker.OutboxPublicationPort;
import com.cryptostrategy.platform.persistence.api.worker.OutboxRecord;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class OutboxPublisherEngine {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherEngine.class);

    private final OutboxPublicationPort outboxPort;
    private final RedisStreamPublisher streamPublisher;
    private final WorkerProperties workerProperties;

    public OutboxPublisherEngine(
            OutboxPublicationPort outboxPort,
            RedisStreamPublisher streamPublisher,
            WorkerProperties workerProperties
    ) {
        this.outboxPort = Objects.requireNonNull(outboxPort, "outboxPort cannot be null");
        this.streamPublisher = Objects.requireNonNull(streamPublisher, "streamPublisher cannot be null");
        this.workerProperties = Objects.requireNonNull(workerProperties, "workerProperties cannot be null");
    }

    public int publishPendingOutboxBatch() {
        int batchSize = workerProperties.reconciliation().outboxBatchSize();
        List<OutboxRecord> batch = outboxPort.listUnpublishedBatch(batchSize);
        if (batch.isEmpty()) {
            return 0;
        }

        int publishedCount = 0;
        for (OutboxRecord record : batch) {
            try {
                String streamKey = resolveDestinationStream(record);
                streamPublisher.publish(streamKey, record.messageId(), record.payload(), Map.of(
                        "eventType", record.eventType(),
                        "aggregateType", record.aggregateType() != null ? record.aggregateType() : "",
                        "aggregateId", record.aggregateId() != null ? record.aggregateId() : ""
                ));
                outboxPort.recordPublishSuccess(record.outboxEventId(), Instant.now());
                publishedCount++;
            } catch (Exception ex) {
                log.error("Failed to publish outbox event '{}' to Redis: {}", record.outboxEventId(), ex.getMessage());
                outboxPort.recordPublishFailure(record.outboxEventId(), ex.getMessage(), Instant.now());
            }
        }
        return publishedCount;
    }

    private String resolveDestinationStream(OutboxRecord record) {
        String eventType = record.eventType();
        var streams = workerProperties.streams();
        if (eventType != null && (eventType.equalsIgnoreCase(MessageTypes.BACKTEST_JOB)
                || eventType.equalsIgnoreCase("EXPERIMENT_QUEUED")
                || eventType.toUpperCase().contains("JOB"))) {
            return streams.getBacktestJobsStream();
        }
        return streams.getLifecycleEventsStream();
    }
}

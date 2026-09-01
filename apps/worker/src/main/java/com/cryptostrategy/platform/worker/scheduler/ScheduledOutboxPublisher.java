package com.cryptostrategy.platform.worker.scheduler;

import com.cryptostrategy.platform.worker.engine.OutboxPublisherEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@EnableScheduling
public class ScheduledOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(ScheduledOutboxPublisher.class);

    private final OutboxPublisherEngine outboxPublisherEngine;

    public ScheduledOutboxPublisher(OutboxPublisherEngine outboxPublisherEngine) {
        this.outboxPublisherEngine = Objects.requireNonNull(outboxPublisherEngine, "outboxPublisherEngine cannot be null");
    }

    @Scheduled(fixedDelayString = "${worker.streams.outbox-poll-interval-ms:500}")
    public void scheduleOutboxPublish() {
        try {
            int published = outboxPublisherEngine.publishPendingOutboxBatch();
            if (published > 0) {
                log.debug("Published {} outbox events to Redis streams", published);
            }
        } catch (Exception ex) {
            log.error("Error during scheduled outbox publication: {}", ex.getMessage(), ex);
        }
    }
}

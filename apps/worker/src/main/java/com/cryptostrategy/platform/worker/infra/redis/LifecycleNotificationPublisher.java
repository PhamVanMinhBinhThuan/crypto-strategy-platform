package com.cryptostrategy.platform.worker.infra.redis;

import com.cryptostrategy.platform.contracts.api.LifecycleNotificationPayload;
import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.domain.api.identity.Ulids;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Component
public class LifecycleNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(LifecycleNotificationPublisher.class);

    private final RedisStreamPublisher streamPublisher;
    private final WorkerProperties workerProperties;
    private final ObjectMapper objectMapper;

    public LifecycleNotificationPublisher(
            RedisStreamPublisher streamPublisher,
            WorkerProperties workerProperties,
            ObjectMapper objectMapper
    ) {
        this.streamPublisher = Objects.requireNonNull(streamPublisher, "streamPublisher cannot be null");
        this.workerProperties = Objects.requireNonNull(workerProperties, "workerProperties cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    public void publishLifecycleNotification(
            String aggregateType,
            String aggregateId,
            String experimentId,
            String jobId,
            String candidateId,
            String lifecycleEventType,
            String correlationId
    ) {
        try {
            LifecycleNotificationPayload payload = new LifecycleNotificationPayload(
                    aggregateType != null ? aggregateType : "EXPERIMENT",
                    aggregateId,
                    experimentId,
                    jobId,
                    candidateId,
                    lifecycleEventType != null ? lifecycleEventType : "STATE_CHANGED"
            );

            MessageEnvelope<LifecycleNotificationPayload> envelope = new MessageEnvelope<>(
                    Ulids.generate(),
                    1,
                    MessageTypes.LIFECYCLE_NOTIFICATION,
                    Instant.now(),
                    correlationId != null ? correlationId : aggregateId,
                    payload
            );

            String serialized = objectMapper.writeValueAsString(envelope);
            String lifecycleStream = workerProperties.streams().getLifecycleEventsStream();

            streamPublisher.publish(lifecycleStream, envelope.messageId(), serialized, Map.of(
                    "messageType", MessageTypes.LIFECYCLE_NOTIFICATION,
                    "correlationId", envelope.correlationId()
            ));
            log.debug("Published LifecycleNotification for aggregate '{}' to stream '{}'", aggregateId, lifecycleStream);
        } catch (Exception ex) {
            log.error("Failed to publish LifecycleNotification for aggregate '{}': {}", aggregateId, ex.getMessage(), ex);
        }
    }
}

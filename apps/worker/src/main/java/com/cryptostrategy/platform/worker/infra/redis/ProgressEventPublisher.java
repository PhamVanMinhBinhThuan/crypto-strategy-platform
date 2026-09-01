package com.cryptostrategy.platform.worker.infra.redis;

import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.contracts.api.ProgressEventPayload;
import com.cryptostrategy.platform.domain.api.identity.Ulids;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Component
public class ProgressEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProgressEventPublisher.class);

    private final RedisStreamPublisher streamPublisher;
    private final WorkerProperties workerProperties;
    private final ObjectMapper objectMapper;

    public ProgressEventPublisher(
            RedisStreamPublisher streamPublisher,
            WorkerProperties workerProperties,
            ObjectMapper objectMapper
    ) {
        this.streamPublisher = Objects.requireNonNull(streamPublisher, "streamPublisher cannot be null");
        this.workerProperties = Objects.requireNonNull(workerProperties, "workerProperties cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    public void publishProgress(
            String experimentId,
            String jobId,
            int completedWork,
            int failedWork,
            int totalWork,
            BigDecimal bestScore,
            String leaderboardRevisionId,
            String eventType,
            String correlationId
    ) {
        try {
            ProgressEventPayload payload = new ProgressEventPayload(
                    experimentId,
                    jobId,
                    completedWork,
                    failedWork,
                    totalWork,
                    bestScore,
                    leaderboardRevisionId,
                    eventType != null ? eventType : "PROGRESS_UPDATED"
            );

            MessageEnvelope<ProgressEventPayload> envelope = new MessageEnvelope<>(
                    Ulids.generate(),
                    1,
                    MessageTypes.PROGRESS_EVENT,
                    Instant.now(),
                    correlationId != null ? correlationId : jobId,
                    payload
            );

            String serialized = objectMapper.writeValueAsString(envelope);
            String progressStream = workerProperties.streams().getProgressEventsStream();

            streamPublisher.publish(progressStream, envelope.messageId(), serialized, Map.of(
                    "messageType", MessageTypes.PROGRESS_EVENT,
                    "correlationId", envelope.correlationId()
            ));
            log.debug("Published ProgressEvent for job '{}' to stream '{}'", jobId, progressStream);
        } catch (Exception ex) {
            log.error("Failed to publish ProgressEvent for job '{}': {}", jobId, ex.getMessage(), ex);
        }
    }
}

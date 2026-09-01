package com.cryptostrategy.platform.worker.infra.redis;

import com.cryptostrategy.platform.contracts.api.DeadLetterPayload;
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
public class DeadLetterPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterPublisher.class);

    private final RedisStreamPublisher streamPublisher;
    private final WorkerProperties workerProperties;
    private final ObjectMapper objectMapper;

    public DeadLetterPublisher(
            RedisStreamPublisher streamPublisher,
            WorkerProperties workerProperties,
            ObjectMapper objectMapper
    ) {
        this.streamPublisher = Objects.requireNonNull(streamPublisher, "streamPublisher cannot be null");
        this.workerProperties = Objects.requireNonNull(workerProperties, "workerProperties cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    public void publishDeadLetter(
            String experimentId,
            String jobId,
            String candidateId,
            String originalMessageId,
            String failureClassification,
            String failureCode,
            String safeDiagnosticReference,
            int attemptCount
    ) {
        try {
            DeadLetterPayload payload = new DeadLetterPayload(
                    experimentId,
                    jobId,
                    candidateId,
                    originalMessageId,
                    failureClassification != null ? failureClassification : "UNKNOWN_ERROR",
                    failureCode != null ? failureCode : "UNKNOWN",
                    safeDiagnosticReference != null ? safeDiagnosticReference : "",
                    attemptCount,
                    Instant.now()
            );

            MessageEnvelope<DeadLetterPayload> envelope = new MessageEnvelope<>(
                    Ulids.generate(),
                    1,
                    MessageTypes.DEAD_LETTER,
                    Instant.now(),
                    originalMessageId,
                    payload
            );

            String serialized = objectMapper.writeValueAsString(envelope);
            String deadLetterStream = workerProperties.streams().getDeadLetterStream();

            streamPublisher.publish(deadLetterStream, envelope.messageId(), serialized, Map.of(
                    "messageType", MessageTypes.DEAD_LETTER,
                    "correlationId", originalMessageId
            ));
            log.warn("Published message '{}' to Dead-Letter Stream '{}' with failureCode: {}", originalMessageId, deadLetterStream, failureCode);
        } catch (Exception ex) {
            log.error("Failed to publish dead letter for message '{}': {}", originalMessageId, ex.getMessage(), ex);
        }
    }
}

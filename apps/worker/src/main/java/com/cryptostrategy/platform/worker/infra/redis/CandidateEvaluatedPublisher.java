package com.cryptostrategy.platform.worker.infra.redis;

import com.cryptostrategy.platform.contracts.api.CandidateEvaluatedPayload;
import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
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
public class CandidateEvaluatedPublisher {

    private static final Logger log = LoggerFactory.getLogger(CandidateEvaluatedPublisher.class);

    private final RedisStreamPublisher streamPublisher;
    private final WorkerProperties workerProperties;
    private final ObjectMapper objectMapper;

    public CandidateEvaluatedPublisher(
            RedisStreamPublisher streamPublisher,
            WorkerProperties workerProperties,
            ObjectMapper objectMapper
    ) {
        this.streamPublisher = Objects.requireNonNull(streamPublisher, "streamPublisher cannot be null");
        this.workerProperties = Objects.requireNonNull(workerProperties, "workerProperties cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    public void publishCandidateEvaluated(
            String experimentId,
            String jobId,
            String candidateId,
            String backtestResultId,
            String evaluationResultId,
            BigDecimal overallScore,
            String correlationId
    ) {
        try {
            CandidateEvaluatedPayload payload = new CandidateEvaluatedPayload(
                    experimentId,
                    jobId,
                    candidateId,
                    backtestResultId,
                    evaluationResultId,
                    overallScore
            );

            MessageEnvelope<CandidateEvaluatedPayload> envelope = new MessageEnvelope<>(
                    Ulids.generate(),
                    1,
                    MessageTypes.CANDIDATE_EVALUATED,
                    Instant.now(),
                    correlationId != null ? correlationId : jobId,
                    payload
            );

            String serialized = objectMapper.writeValueAsString(envelope);
            String candidateStream = workerProperties.streams().getCandidateEvaluatedStream();

            streamPublisher.publish(candidateStream, envelope.messageId(), serialized, Map.of(
                    "messageType", MessageTypes.CANDIDATE_EVALUATED,
                    "correlationId", envelope.correlationId()
            ));
            log.debug("Published CandidateEvaluated event for candidate '{}' to stream '{}'", candidateId, candidateStream);
        } catch (Exception ex) {
            log.error("Failed to publish CandidateEvaluated event for candidate '{}': {}", candidateId, ex.getMessage(), ex);
        }
    }
}

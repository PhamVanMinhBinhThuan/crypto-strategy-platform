package com.cryptostrategy.platform.worker.search.consumer;

import com.cryptostrategy.platform.contracts.api.CandidateEvaluatedPayload;
import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamMessageReader;
import com.cryptostrategy.platform.worker.search.coordination.SearchCoordinator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.scheduling.annotation.Scheduled;

/** Consumer Candidate Evaluated của Search dùng group riêng với ranking consumer. */
public final class SearchCompletionConsumer {
    private static final Logger log = LoggerFactory.getLogger(SearchCompletionConsumer.class);
    private final RedisStreamMessageReader reader;
    private final SearchCoordinator coordinator;
    private final WorkerProperties properties;
    private final ObjectMapper mapper;
    private final JavaType envelopeType;

    public SearchCompletionConsumer(
            RedisStreamMessageReader reader,
            SearchCoordinator coordinator,
            WorkerProperties properties,
            ObjectMapper mapper) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.envelopeType = mapper.getTypeFactory()
                .constructParametricType(MessageEnvelope.class, CandidateEvaluatedPayload.class);
    }

    @Scheduled(fixedDelayString = "${worker.search.completion-poll-interval-ms:500}")
    public void pollCompletions() {
        String stream = properties.streams().getCandidateEvaluatedStream();
        String group = properties.consumer().searchGroup();
        try {
            process(reader.readPendingBatch(stream, group, properties.consumer().consumerName(),
                    properties.consumer().pendingBatchSize()), stream, group);
            process(reader.readGroupBatch(stream, group, properties.consumer().consumerName(),
                    properties.consumer().readBatchSize(), properties.consumer().pollTimeout()), stream, group);
        } catch (RuntimeException failure) {
            log.warn("Search completion polling failed for stream '{}': {}", stream, failure.getMessage());
        }
    }

    private void process(List<MapRecord<String, String, String>> records, String stream, String group) {
        for (MapRecord<String, String, String> record : records) {
            try {
                MessageEnvelope<CandidateEvaluatedPayload> envelope = mapper.readValue(
                        required(record, "payload"), envelopeType);
                if (envelope.messageVersion() != MessageTypes.CURRENT_VERSION
                        || !MessageTypes.CANDIDATE_EVALUATED.equals(envelope.messageType())) {
                    throw new IllegalArgumentException("Unsupported Candidate Evaluated envelope");
                }
                CandidateEvaluatedPayload payload = envelope.payload();
                coordinator.complete(new TrustedSearchCoordinationUseCase.CompletionTrigger(
                        envelope.messageId(), new com.cryptostrategy.platform.experiment.api.ExperimentId(payload.experimentId().value()), new com.cryptostrategy.platform.experiment.api.CandidateId(payload.candidateId().value()),
                        new com.cryptostrategy.platform.experiment.api.job.JobId(payload.jobId().value()), envelope.occurredAt(), envelope.correlationId()));
                reader.ack(stream, group, record.getId());
            } catch (Exception failure) {
                log.warn("Search completion '{}' remains pending: {}", record.getId(), failure.getMessage());
            }
        }
    }

    private static String required(MapRecord<String, String, String> record, String key) {
        String value = record.getValue().get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing stream field: " + key);
        return value;
    }
}

package com.cryptostrategy.platform.worker.search.consumer;

import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.contracts.api.SearchRequestPayload;
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

/** Consumer group riêng cho Search; ACK chỉ sau durable coordination thành công. */
public final class SearchRequestConsumer {
    private static final Logger log = LoggerFactory.getLogger(SearchRequestConsumer.class);
    private final RedisStreamMessageReader reader;
    private final SearchCoordinator coordinator;
    private final WorkerProperties properties;
    private final ObjectMapper mapper;
    private final JavaType envelopeType;

    public SearchRequestConsumer(
            RedisStreamMessageReader reader,
            SearchCoordinator coordinator,
            WorkerProperties properties,
            ObjectMapper mapper) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.envelopeType = mapper.getTypeFactory()
                .constructParametricType(MessageEnvelope.class, SearchRequestPayload.class);
    }

    @Scheduled(fixedDelayString = "${worker.search.poll-interval-ms:500}")
    public void pollSearchRequests() {
        String stream = properties.streams().getSearchRequestsStream();
        String group = properties.consumer().searchGroup();
        try {
            process(reader.readPendingBatch(
                    stream, group, properties.consumer().consumerName(),
                    properties.consumer().pendingBatchSize()), stream, group);
            process(reader.readGroupBatch(
                    stream, group, properties.consumer().consumerName(),
                    properties.consumer().readBatchSize(), properties.consumer().pollTimeout()), stream, group);
        } catch (RuntimeException failure) {
            log.warn("Search request polling failed for stream '{}': {}", stream, failure.getMessage());
        }
    }

    private void process(
            List<MapRecord<String, String, String>> records,
            String stream,
            String group) {
        for (MapRecord<String, String, String> record : records) {
            try {
                MessageEnvelope<SearchRequestPayload> envelope = mapper.readValue(
                        required(record, "payload"), envelopeType);
                if (envelope.messageVersion() != MessageTypes.CURRENT_VERSION
                        || !MessageTypes.SEARCH_REQUEST.equals(envelope.messageType())) {
                    throw new IllegalArgumentException("Unsupported Search Request envelope");
                }
                coordinator.coordinate(envelope.payload(), envelope.correlationId());
                reader.ack(stream, group, record.getId());
            } catch (Exception failure) {
                log.warn("Search request '{}' remains pending: {}", record.getId(), failure.getMessage());
            }
        }
    }

    private static String required(MapRecord<String, String, String> record, String key) {
        String value = record.getValue().get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing stream field: " + key);
        return value;
    }
}

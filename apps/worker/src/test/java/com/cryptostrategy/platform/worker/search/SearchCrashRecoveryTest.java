package com.cryptostrategy.platform.worker.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.contracts.api.SearchRequestPayload;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamMessageReader;
import com.cryptostrategy.platform.worker.search.consumer.SearchRequestConsumer;
import com.cryptostrategy.platform.worker.search.coordination.SearchCoordinator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;

class SearchCrashRecoveryTest {
    @Test
    void unackedKillPointIsReclaimedAndAckedAfterDurableRetry() throws Exception {
        RedisStreamMessageReader reader = mock(RedisStreamMessageReader.class);
        SearchCoordinator coordinator = mock(SearchCoordinator.class);
        WorkerProperties properties = new WorkerProperties(null, null, null, null, null, null, null, null);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        SearchRequestConsumer consumer = new SearchRequestConsumer(reader, coordinator, properties, mapper);
        SearchRequestPayload payload = new SearchRequestPayload(
                "01J7K8M9N0P1Q2R3S4T5A6V7W2", "01J7K8M9N0P1Q2R3S4T5A6V7W3", 1, 2);
        MessageEnvelope<SearchRequestPayload> envelope = new MessageEnvelope<>(
                "01J7K8M9N0P1Q2R3S4T5A6V7W1", 1, MessageTypes.SEARCH_REQUEST,
                Instant.parse("2026-09-03T00:00:00Z"), "correlation-f010", payload);
        MapRecord<String, String, String> record = MapRecord.create(
                properties.streams().getSearchRequestsStream(),
                Map.of("payload", mapper.writeValueAsString(envelope))).withId(RecordId.of("1700000000000-0"));
        when(reader.readPendingBatch(any(), any(), any(), anyInt())).thenReturn(List.of(record));
        when(reader.readGroupBatch(any(), any(), any(), anyInt(), any())).thenReturn(List.of());
        doThrow(new IllegalStateException("kill point")).when(coordinator).coordinate(any(), eq("correlation-f010"));

        consumer.pollSearchRequests();
        verify(reader, never()).ack(any(), any(), any(RecordId[].class));

        reset(coordinator);
        consumer.pollSearchRequests();
        verify(coordinator).coordinate(payload, "correlation-f010");
        verify(reader).ack(properties.streams().getSearchRequestsStream(),
                properties.consumer().searchGroup(), record.getId());
    }
}

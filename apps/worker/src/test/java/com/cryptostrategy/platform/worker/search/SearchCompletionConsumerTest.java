package com.cryptostrategy.platform.worker.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.contracts.api.CandidateEvaluatedPayload;
import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamMessageReader;
import com.cryptostrategy.platform.worker.search.consumer.SearchCompletionConsumer;
import com.cryptostrategy.platform.worker.search.coordination.SearchCoordinator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;

class SearchCompletionConsumerTest {
    private static final String EXPERIMENT = "01J7K8M9N0P1Q2R3S4T5A6V7W2";
    private static final String JOB = "01J7K8M9N0P1Q2R3S4T5A6V7W3";
    private static final String CANDIDATE = "01J7K8M9N0P1Q2R3S4T5A6V7W4";
    private static final String RESULT = "01J7K8M9N0P1Q2R3S4T5A6V7W5";
    private static final String EVALUATION = "01J7K8M9N0P1Q2R3S4T5A6V7W6";

    @Test
    void duplicateAndOutOfOrderTriggersReachDurableReconciliationAndAckIndependently() throws Exception {
        RedisStreamMessageReader reader = mock(RedisStreamMessageReader.class);
        SearchCoordinator coordinator = mock(SearchCoordinator.class);
        WorkerProperties properties = new WorkerProperties(null, null, null, null, null, null, null, null);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var consumer = new SearchCompletionConsumer(reader, coordinator, properties, mapper);
        List<MapRecord<String, String, String>> records = List.of(
                record(mapper, "01J7K8M9N0P1Q2R3S4T5A6V7X1", "1700000000000-0", Instant.parse("2026-09-03T00:00:02Z")),
                record(mapper, "01J7K8M9N0P1Q2R3S4T5A6V7X1", "1700000000000-1", Instant.parse("2026-09-03T00:00:02Z")),
                record(mapper, "01J7K8M9N0P1Q2R3S4T5A6V7X2", "1700000000000-2", Instant.parse("2026-09-03T00:00:01Z")));
        when(reader.readPendingBatch(any(), any(), any(), anyInt())).thenReturn(records);
        when(reader.readGroupBatch(any(), any(), any(), anyInt(), any())).thenReturn(List.of());

        consumer.pollCompletions();

        ArgumentCaptor<TrustedSearchCoordinationUseCase.CompletionTrigger> triggers =
                ArgumentCaptor.forClass(TrustedSearchCoordinationUseCase.CompletionTrigger.class);
        verify(coordinator, times(3)).complete(triggers.capture());
        assertThat(triggers.getAllValues()).extracting(TrustedSearchCoordinationUseCase.CompletionTrigger::messageId)
                .containsExactly("01J7K8M9N0P1Q2R3S4T5A6V7X1", "01J7K8M9N0P1Q2R3S4T5A6V7X1", "01J7K8M9N0P1Q2R3S4T5A6V7X2");
        for (MapRecord<String, String, String> record : records) {
            verify(reader).ack(properties.streams().getCandidateEvaluatedStream(),
                    properties.consumer().searchGroup(), record.getId());
        }
    }

    private static MapRecord<String, String, String> record(
            ObjectMapper mapper, String messageId, String recordId, Instant occurredAt) throws Exception {
        var payload = new CandidateEvaluatedPayload(
                EXPERIMENT, JOB, CANDIDATE, RESULT, EVALUATION, new BigDecimal("0.42"));
        var envelope = new MessageEnvelope<>(messageId, 1, MessageTypes.CANDIDATE_EVALUATED,
                occurredAt, "correlation-f010", payload);
        return MapRecord.create("candidate.evaluated.v1",
                Map.of("payload", mapper.writeValueAsString(envelope))).withId(RecordId.of(recordId));
    }
}

package com.cryptostrategy.platform.worker.consumer;

import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.engine.BacktestExecutionPipeline;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamMessageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CandidateEvaluatedConsumerTest {

    private RedisStreamMessageReader messageReader;
    private MessageDispatcher messageDispatcher;
    private BacktestExecutionPipeline executionPipeline;
    private WorkerProperties workerProperties;
    private CandidateEvaluatedConsumer consumer;

    @BeforeEach
    void setUp() {
        messageReader = mock(RedisStreamMessageReader.class);
        messageDispatcher = mock(MessageDispatcher.class);
        executionPipeline = mock(BacktestExecutionPipeline.class);
        workerProperties = new WorkerProperties(null, null, null, null, null, null, null, null);
        consumer = new CandidateEvaluatedConsumer(messageReader, messageDispatcher, executionPipeline, workerProperties);
    }

    @Test
    void pollCandidateEvaluatedReadsAndDispatchesRecords() {
        MapRecord<String, String, String> record = MapRecord.create(
                workerProperties.streams().getCandidateEvaluatedStream(),
                Map.of("messageId", "01J7K8M9N0P1Q2R3S4T5A6V7W1", "payload", "{}")
        ).withId(RecordId.of("1700000000000-0"));

        when(messageReader.readPendingBatch(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());
        when(messageReader.readGroupBatch(anyString(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(List.of(record));

        consumer.pollCandidateEvaluated();

        verify(executionPipeline).executeAsync(any(Runnable.class));
    }
}

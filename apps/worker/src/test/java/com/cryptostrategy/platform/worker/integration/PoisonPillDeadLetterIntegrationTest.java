package com.cryptostrategy.platform.worker.integration;

import com.cryptostrategy.platform.backtesting.api.port.in.PrepareBacktestUseCase;
import com.cryptostrategy.platform.execution.api.port.in.CompleteBacktestAttemptUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.consumer.BacktestJobHandler;
import com.cryptostrategy.platform.worker.consumer.DualLayerIdempotencyGuard;
import com.cryptostrategy.platform.worker.infra.redis.CandidateEvaluatedPublisher;
import com.cryptostrategy.platform.worker.infra.redis.DeadLetterPublisher;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamMessageReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PoisonPillDeadLetterIntegrationTest {

    private DualLayerIdempotencyGuard idempotencyGuard;
    private TrustedWorkerExperimentUseCase experimentUseCase;
    private PrepareBacktestUseCase prepareBacktestUseCase;
    private CompleteBacktestAttemptUseCase completeBacktestAttemptUseCase;
    private CandidateEvaluatedPublisher candidateEvaluatedPublisher;
    private DeadLetterPublisher deadLetterPublisher;
    private RedisStreamMessageReader messageReader;
    private WorkerProperties workerProperties;
    private ObjectMapper objectMapper;
    private BacktestJobHandler handler;

    @BeforeEach
    void setUp() {
        idempotencyGuard = mock(DualLayerIdempotencyGuard.class);
        experimentUseCase = mock(TrustedWorkerExperimentUseCase.class);
        prepareBacktestUseCase = mock(PrepareBacktestUseCase.class);
        completeBacktestAttemptUseCase = mock(CompleteBacktestAttemptUseCase.class);
        candidateEvaluatedPublisher = mock(CandidateEvaluatedPublisher.class);
        deadLetterPublisher = mock(DeadLetterPublisher.class);
        messageReader = mock(RedisStreamMessageReader.class);
        workerProperties = new WorkerProperties(null, null, null, null, null, null, null, null);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        handler = new BacktestJobHandler(
                idempotencyGuard,
                experimentUseCase,
                prepareBacktestUseCase,
                completeBacktestAttemptUseCase,
                candidateEvaluatedPublisher,
                deadLetterPublisher,
                messageReader,
                workerProperties,
                objectMapper
        );
    }

    @Test
    void malformedPayloadRoutesToDeadLetterStreamAndAcksWithoutCrashing() {
        MapRecord<String, String, String> record = MapRecord.create(
                workerProperties.streams().getBacktestJobsStream(),
                Map.of("messageId", "01J7K8M9N0P1Q2R3S4T5A6V7W1", "payload", "{invalid_json: true")
        ).withId(RecordId.of("1700000000003-0"));

        handler.handle(record);

        verify(deadLetterPublisher).publishDeadLetter(
                anyString(), anyString(), any(), eq("01J7K8M9N0P1Q2R3S4T5A6V7W1"),
                eq("PERMANENT_LOGIC_ERROR"), eq("MALFORMED_PAYLOAD"), anyString(), anyInt()
        );
        verify(experimentUseCase, never()).startNextAttempt(any(), any());
        verify(messageReader).ack(any(), any(), eq(record.getId()));
    }
}

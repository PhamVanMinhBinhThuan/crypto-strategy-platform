package com.cryptostrategy.platform.worker.consumer;

import com.cryptostrategy.platform.backtesting.api.PreparedBacktestOutcome;
import com.cryptostrategy.platform.backtesting.api.model.BacktestAssumptions;
import com.cryptostrategy.platform.backtesting.api.model.BacktestProvenance;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.backtesting.api.model.EquityCurveSummary;
import com.cryptostrategy.platform.backtesting.api.model.Money;
import com.cryptostrategy.platform.backtesting.api.port.in.PrepareBacktestUseCase;
import com.cryptostrategy.platform.contracts.api.BacktestJobPayload;
import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.execution.api.BacktestCompletionOutcome;
import com.cryptostrategy.platform.execution.api.port.in.CompleteBacktestAttemptUseCase;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.AttemptStatus;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.infra.redis.CandidateEvaluatedPublisher;
import com.cryptostrategy.platform.worker.infra.redis.DeadLetterPublisher;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamMessageReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BacktestJobHandlerTest {

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
    void handlesBacktestJobSuccessfully() throws Exception {
        String msgId = "01J7K8M9N0P1Q2R3S4T5A6V7W1";
        String expId = "01J7K8M9N0P1Q2R3S4T5A6V7W2";
        String candId = "01J7K8M9N0P1Q2R3S4T5A6V7W3";
        String jId = "01J7K8M9N0P1Q2R3S4T5A6V7W4";

        BacktestJobPayload payload = new BacktestJobPayload(expId, jId, candId);
        MessageEnvelope<BacktestJobPayload> envelope = new MessageEnvelope<>(
                msgId, 1, MessageTypes.BACKTEST_JOB, Instant.now(), "corr-1", payload
        );
        String rawJson = objectMapper.writeValueAsString(envelope);

        MapRecord<String, String, String> record = MapRecord.create(
                workerProperties.streams().getBacktestJobsStream(),
                Map.of("messageId", msgId, "payload", rawJson)
        ).withId(RecordId.of("1700000000000-0"));

        when(idempotencyGuard.isAlreadyProcessed(any(), eq(msgId))).thenReturn(false);

        ExecutionAttempt attempt = ExecutionAttempt.start(
                AttemptId.generate(), new JobId(jId), new CandidateId(candId), 1,
                "worker-1", Instant.now()
        );
        when(experimentUseCase.startNextAttempt(eq(new JobId(jId)), any())).thenReturn(attempt);

        BacktestAssumptions assumptions = BacktestAssumptions.mvp(BigDecimal.valueOf(10000), BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.0005));
        EquityCurveSummary summary = new EquityCurveSummary(100L, Money.of(BigDecimal.valueOf(12000)), Money.of(BigDecimal.valueOf(9500)), 10L, 50L, "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        BacktestResult result = new BacktestResult(
                BacktestResultId.generate(), new ExperimentId(expId), new CandidateId(candId), new JobId(jId),
                attempt.attemptId(), new BacktestProvenance("fp", "ds-cs", "st-fp"), assumptions,
                Money.of(BigDecimal.valueOf(10000)), Money.of(BigDecimal.valueOf(12000)), Money.of(BigDecimal.ZERO),
                List.of(), summary, "res-fp", Instant.now()
        );
        PreparedBacktestOutcome prepared = new PreparedBacktestOutcome(result, List.of());
        when(prepareBacktestUseCase.prepare(any())).thenReturn(prepared);

        BacktestCompletionOutcome outcome = new BacktestCompletionOutcome(
                new ExperimentId(expId), new JobId(jId), new CandidateId(candId),
                result.resultId(), EvaluationResultId.generate(), BigDecimal.valueOf(0.85)
        );
        when(completeBacktestAttemptUseCase.completeAttempt(eq(new JobId(jId)), eq(attempt.attemptId()), eq(prepared)))
                .thenReturn(outcome);

        handler.handle(record);

        verify(candidateEvaluatedPublisher).publishCandidateEvaluated(eq(expId), eq(jId), eq(candId), any(), any(), any(), eq("corr-1"));
        verify(idempotencyGuard).markProcessed(any(), eq(msgId), any());
        verify(messageReader).ack(any(), any(), eq(record.getId()));
    }
}

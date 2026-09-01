package com.cryptostrategy.platform.worker.consumer;

import com.cryptostrategy.platform.contracts.api.CandidateEvaluatedPayload;
import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.TerminalWorkOutcome;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevision;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevisionId;
import com.cryptostrategy.platform.leaderboard.api.port.in.LeaderboardReconciliationUseCase;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.infra.redis.LifecycleNotificationPublisher;
import com.cryptostrategy.platform.worker.infra.redis.ProgressEventPublisher;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CandidateEvaluatedHandlerTest {

    private DualLayerIdempotencyGuard idempotencyGuard;
    private TrustedWorkerExperimentUseCase experimentUseCase;
    private LeaderboardReconciliationUseCase leaderboardReconciliationUseCase;
    private ProgressEventPublisher progressEventPublisher;
    private LifecycleNotificationPublisher lifecycleNotificationPublisher;
    private RedisStreamMessageReader messageReader;
    private WorkerProperties workerProperties;
    private ObjectMapper objectMapper;
    private CandidateEvaluatedHandler handler;

    @BeforeEach
    void setUp() {
        idempotencyGuard = mock(DualLayerIdempotencyGuard.class);
        experimentUseCase = mock(TrustedWorkerExperimentUseCase.class);
        leaderboardReconciliationUseCase = mock(LeaderboardReconciliationUseCase.class);
        progressEventPublisher = mock(ProgressEventPublisher.class);
        lifecycleNotificationPublisher = mock(LifecycleNotificationPublisher.class);
        messageReader = mock(RedisStreamMessageReader.class);
        workerProperties = new WorkerProperties(null, null, null, null, null, null, null, null);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        handler = new CandidateEvaluatedHandler(
                idempotencyGuard,
                experimentUseCase,
                leaderboardReconciliationUseCase,
                progressEventPublisher,
                lifecycleNotificationPublisher,
                messageReader,
                workerProperties,
                objectMapper
        );
    }

    @Test
    void handlesCandidateEvaluatedSuccessfully() throws Exception {
        String msgId = "01J7K8M9N0P1Q2R3S4T5A6V7W1";
        String expId = "01J7K8M9N0P1Q2R3S4T5A6V7W2";
        String jId = "01J7K8M9N0P1Q2R3S4T5A6V7W3";
        String candId = "01J7K8M9N0P1Q2R3S4T5A6V7W4";
        String btId = "01J7K8M9N0P1Q2R3S4T5A6V7W5";
        String evalId = "01J7K8M9N0P1Q2R3S4T5A6V7W6";

        CandidateEvaluatedPayload payload = new CandidateEvaluatedPayload(
                expId, jId, candId, btId, evalId, BigDecimal.valueOf(0.85)
        );
        MessageEnvelope<CandidateEvaluatedPayload> envelope = new MessageEnvelope<>(
                msgId, 1, MessageTypes.CANDIDATE_EVALUATED, Instant.now(), "corr-1", payload
        );
        String rawJson = objectMapper.writeValueAsString(envelope);

        MapRecord<String, String, String> record = MapRecord.create(
                workerProperties.streams().getCandidateEvaluatedStream(),
                Map.of("messageId", msgId, "payload", rawJson)
        ).withId(RecordId.of("1700000000000-0"));

        when(idempotencyGuard.isAlreadyProcessed(any(), eq(msgId))).thenReturn(false);

        LeaderboardRevision revision = new LeaderboardRevision(
                LeaderboardRevisionId.generate(), new ExperimentId(expId), 1L, 10,
                new com.cryptostrategy.platform.evaluation.api.model.RankingVersion("1.0.0"),
                List.of(), "rev-fp", Instant.now()
        );
        when(leaderboardReconciliationUseCase.reconcileLeaderboard(eq(new ExperimentId(expId)), anyInt()))
                .thenReturn(Optional.of(revision));
        when(experimentUseCase.getExperimentStatus(eq(new ExperimentId(expId)))).thenReturn(ExperimentStatus.COMPLETED);

        handler.handle(record);

        verify(experimentUseCase).recordTerminalProgress(eq(new JobId(jId)), eq(TerminalWorkOutcome.SUCCEEDED), eq(BigDecimal.valueOf(0.85)));
        verify(progressEventPublisher).publishProgress(eq(expId), eq(jId), eq(1), eq(0), eq(1), eq(BigDecimal.valueOf(0.85)), eq(revision.revisionId().value()), any(), eq("corr-1"));
        verify(lifecycleNotificationPublisher).publishLifecycleNotification(eq("EXPERIMENT"), eq(expId), eq(expId), eq(jId), eq(candId), eq("COMPLETED"), eq("corr-1"));
        verify(idempotencyGuard).markProcessed(any(), eq(msgId), any());
        verify(messageReader).ack(any(), any(), eq(record.getId()));
    }
}

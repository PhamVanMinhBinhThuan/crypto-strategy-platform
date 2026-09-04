package com.cryptostrategy.platform.worker.engine;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.DueRetryJob;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.RecoverableQueuedJob;
import com.cryptostrategy.platform.experiment.api.job.StaleRunningAttempt;
import com.cryptostrategy.platform.experiment.api.job.StopCandidateExperiment;
import com.cryptostrategy.platform.experiment.api.port.in.CompleteStoppedExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerRecoveryQueryUseCase;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.infra.redis.LifecycleNotificationPublisher;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class RecoverySweeperEngineTest {

    private TrustedWorkerRecoveryQueryUseCase recoveryQueryUseCase;
    private TrustedWorkerExperimentUseCase experimentUseCase;
    private CompleteStoppedExperimentUseCase completeStoppedExperimentUseCase;
    private RedisStreamPublisher streamPublisher;
    private LifecycleNotificationPublisher lifecycleNotificationPublisher;
    private WorkerProperties workerProperties;
    private ObjectMapper objectMapper;
    private RecoverySweeperEngine engine;

    @BeforeEach
    void setUp() {
        recoveryQueryUseCase = mock(TrustedWorkerRecoveryQueryUseCase.class);
        experimentUseCase = mock(TrustedWorkerExperimentUseCase.class);
        completeStoppedExperimentUseCase = mock(CompleteStoppedExperimentUseCase.class);
        streamPublisher = mock(RedisStreamPublisher.class);
        lifecycleNotificationPublisher = mock(LifecycleNotificationPublisher.class);
        workerProperties = new WorkerProperties(null, null, null, null, null, null, null, null);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        engine = new RecoverySweeperEngine(
                recoveryQueryUseCase,
                experimentUseCase,
                completeStoppedExperimentUseCase,
                streamPublisher,
                lifecycleNotificationPublisher,
                workerProperties,
                objectMapper
        );
    }

    @Test
    void sweepsUnqueuedJobsAndPublishesToStream() {
        RecoverableQueuedJob job = new RecoverableQueuedJob(
                new JobId("01J7K8M9N0P1Q2R3S4T5A6V7W1"),
                new ExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W2"),
                new CandidateId("01J7K8M9N0P1Q2R3S4T5A6V7W3"),
                Instant.now().minusSeconds(120)
        );
        when(recoveryQueryUseCase.findRecoverableQueuedJobs(any(), anyInt())).thenReturn(List.of(job));

        int count = engine.sweepUnqueuedJobs();

        assertThat(count).isEqualTo(1);
        verify(streamPublisher).publish(
                eq(workerProperties.streams().getBacktestJobsStream()),
                eq(job.jobId().value()),
                any(),
                any());
    }

    @Test
    void sweepsDueRetriesAndRequeues() {
        DueRetryJob due = new DueRetryJob(
                new JobId("01J7K8M9N0P1Q2R3S4T5A6V7W1"),
                new ExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W2"),
                new CandidateId("01J7K8M9N0P1Q2R3S4T5A6V7W3"),
                Instant.now().minusSeconds(10)
        );
        when(recoveryQueryUseCase.findDueRetries(any(), anyInt())).thenReturn(List.of(due));

        int count = engine.sweepDueRetries();

        assertThat(count).isEqualTo(1);
        verify(experimentUseCase).requeueDueRetry(eq(due.jobId()));
        verify(streamPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void sweepsStaleAttemptsAndFinalizesTimeout() {
        StaleRunningAttempt stale = new StaleRunningAttempt(
                new JobId("01J7K8M9N0P1Q2R3S4T5A6V7W1"),
                AttemptId.generate(),
                new ExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W2"),
                new CandidateId("01J7K8M9N0P1Q2R3S4T5A6V7W3"),
                "worker-old",
                1,
                Instant.now().minusSeconds(300)
        );
        when(recoveryQueryUseCase.findStaleRunningAttempts(any(), anyInt())).thenReturn(List.of(stale));

        int count = engine.sweepStaleAttempts();

        assertThat(count).isEqualTo(1);
        verify(experimentUseCase).finalizeFailure(eq(stale.jobId()), eq(stale.attemptId()), eq("STALE_TIMEOUT"), any(), any(), any());
    }

    @Test
    void staleAttemptBecomesTerminalWhenRetryBudgetIsExhausted() {
        StaleRunningAttempt stale = new StaleRunningAttempt(
                new JobId("01J7K8M9N0P1Q2R3S4T5A6V7W1"),
                AttemptId.generate(),
                new ExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W2"),
                new CandidateId("01J7K8M9N0P1Q2R3S4T5A6V7W3"),
                "worker-old",
                workerProperties.retry().maxAttempts(),
                Instant.now().minusSeconds(300)
        );
        when(recoveryQueryUseCase.findStaleRunningAttempts(any(), anyInt())).thenReturn(List.of(stale));

        assertThat(engine.sweepStaleAttempts()).isEqualTo(1);

        verify(experimentUseCase).finalizeFailure(
                eq(stale.jobId()),
                eq(stale.attemptId()),
                eq("STALE_RETRY_EXHAUSTED"),
                any(),
                eq(com.cryptostrategy.platform.experiment.api.job.FailureClassification.UNKNOWN_ERROR),
                eq(null));
    }

    @Test
    void sweepsStoppedExperimentsAndCompletesEligible() {
        StopCandidateExperiment candidate = new StopCandidateExperiment(
                new ExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W1"),
                Instant.now().minusSeconds(60)
        );
        when(recoveryQueryUseCase.findStopCompletionCandidates(anyInt())).thenReturn(List.of(candidate));
        when(completeStoppedExperimentUseCase.completeIfEligible(eq(candidate.experimentId()))).thenReturn(true);

        int count = engine.sweepStoppedExperiments();

        assertThat(count).isEqualTo(1);
        verify(completeStoppedExperimentUseCase).completeIfEligible(eq(candidate.experimentId()));
        verify(lifecycleNotificationPublisher).publishLifecycleNotification(eq("EXPERIMENT"), eq(candidate.experimentId().value()), eq(candidate.experimentId().value()), any(), any(), eq("STOPPED"), any());
    }
}

package com.cryptostrategy.platform.worker.integration;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.DueRetryJob;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.RecoverableQueuedJob;
import com.cryptostrategy.platform.experiment.api.job.StaleRunningAttempt;
import com.cryptostrategy.platform.experiment.api.port.in.CompleteStoppedExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerRecoveryQueryUseCase;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.engine.RecoverySweeperEngine;
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
import static org.mockito.Mockito.when;

class StaleAttemptAndRecoveryIntegrationTest {

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
    void runsRecoverySweepsAndRequeuesPendingWork() {
        RecoverableQueuedJob unqueued = new RecoverableQueuedJob(
                new JobId("01J7K8M9N0P1Q2R3S4T5A6V7W1"),
                new ExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W2"),
                new CandidateId("01J7K8M9N0P1Q2R3S4T5A6V7W3"),
                Instant.now().minusSeconds(180)
        );
        when(recoveryQueryUseCase.findRecoverableQueuedJobs(any(), anyInt())).thenReturn(List.of(unqueued));

        DueRetryJob due = new DueRetryJob(
                new JobId("01J7K8M9N0P1Q2R3S4T5A6V7W4"),
                new ExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W2"),
                new CandidateId("01J7K8M9N0P1Q2R3S4T5A6V7W5"),
                Instant.now().minusSeconds(5)
        );
        when(recoveryQueryUseCase.findDueRetries(any(), anyInt())).thenReturn(List.of(due));

        StaleRunningAttempt stale = new StaleRunningAttempt(
                new JobId("01J7K8M9N0P1Q2R3S4T5A6V7W6"),
                AttemptId.generate(),
                new ExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W2"),
                new CandidateId("01J7K8M9N0P1Q2R3S4T5A6V7W7"),
                "crashed-worker",
                1,
                Instant.now().minusSeconds(360)
        );
        when(recoveryQueryUseCase.findStaleRunningAttempts(any(), anyInt())).thenReturn(List.of(stale));

        int unqueuedCount = engine.sweepUnqueuedJobs();
        int dueCount = engine.sweepDueRetries();
        int staleCount = engine.sweepStaleAttempts();

        assertThat(unqueuedCount).isEqualTo(1);
        assertThat(dueCount).isEqualTo(1);
        assertThat(staleCount).isEqualTo(1);

        verify(experimentUseCase).requeueDueRetry(eq(due.jobId()));
        verify(experimentUseCase).finalizeFailure(eq(stale.jobId()), eq(stale.attemptId()), eq("STALE_TIMEOUT"), any(), any(), any());
    }
}

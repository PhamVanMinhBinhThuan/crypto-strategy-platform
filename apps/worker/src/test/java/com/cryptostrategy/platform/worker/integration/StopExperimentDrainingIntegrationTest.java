package com.cryptostrategy.platform.worker.integration;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.StopCandidateExperiment;
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

class StopExperimentDrainingIntegrationTest {

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
    void finalizesStoppedExperimentAndEmitsLifecycleNotificationWhenDrained() {
        ExperimentId expId = new ExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W1");
        StopCandidateExperiment candidate = new StopCandidateExperiment(expId, Instant.now().minusSeconds(30));
        when(recoveryQueryUseCase.findStopCompletionCandidates(anyInt())).thenReturn(List.of(candidate));
        when(completeStoppedExperimentUseCase.completeIfEligible(eq(expId))).thenReturn(true);

        int stopped = engine.sweepStoppedExperiments();

        assertThat(stopped).isEqualTo(1);
        verify(completeStoppedExperimentUseCase).completeIfEligible(eq(expId));
        verify(lifecycleNotificationPublisher).publishLifecycleNotification(
                eq("EXPERIMENT"), eq(expId.value()), eq(expId.value()), any(), any(), eq("STOPPED"), eq(expId.value())
        );
    }
}

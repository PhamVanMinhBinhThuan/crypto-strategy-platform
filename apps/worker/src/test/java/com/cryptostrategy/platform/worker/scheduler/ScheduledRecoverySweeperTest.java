package com.cryptostrategy.platform.worker.scheduler;

import com.cryptostrategy.platform.worker.engine.RecoverySweeperEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledRecoverySweeperTest {

    private RecoverySweeperEngine engine;
    private ScheduledRecoverySweeper sweeper;

    @BeforeEach
    void setUp() {
        engine = mock(RecoverySweeperEngine.class);
        sweeper = new ScheduledRecoverySweeper(engine);
    }

    @Test
    void sweepUnqueuedJobsDelegatesToEngine() {
        when(engine.sweepUnqueuedJobs()).thenReturn(3);
        sweeper.sweepUnqueuedJobs();
        verify(engine).sweepUnqueuedJobs();
    }

    @Test
    void sweepDueRetriesDelegatesToEngine() {
        when(engine.sweepDueRetries()).thenReturn(2);
        sweeper.sweepDueRetries();
        verify(engine).sweepDueRetries();
    }

    @Test
    void sweepStaleAttemptsDelegatesToEngine() {
        when(engine.sweepStaleAttempts()).thenReturn(1);
        sweeper.sweepStaleAttempts();
        verify(engine).sweepStaleAttempts();
    }

    @Test
    void sweepStoppedExperimentsDelegatesToEngine() {
        when(engine.sweepStoppedExperiments()).thenReturn(1);
        sweeper.sweepStoppedExperiments();
        verify(engine).sweepStoppedExperiments();
    }
}

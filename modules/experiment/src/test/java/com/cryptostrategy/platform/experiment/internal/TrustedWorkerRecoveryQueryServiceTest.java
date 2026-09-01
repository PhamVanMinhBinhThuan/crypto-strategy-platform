package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.DueRetryJob;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.RecoverableQueuedJob;
import com.cryptostrategy.platform.experiment.api.job.StaleRunningAttempt;
import com.cryptostrategy.platform.experiment.api.job.StopCandidateExperiment;
import com.cryptostrategy.platform.experiment.api.port.out.ExecutionAttemptStore;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import com.cryptostrategy.platform.experiment.api.port.out.JobStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrustedWorkerRecoveryQueryServiceTest {

    private JobStore jobStore;
    private ExecutionAttemptStore attemptStore;
    private ExperimentStore experimentStore;
    private TrustedWorkerRecoveryQueryService recoveryQueryService;

    @BeforeEach
    void setUp() {
        jobStore = mock(JobStore.class);
        attemptStore = mock(ExecutionAttemptStore.class);
        experimentStore = mock(ExperimentStore.class);

        recoveryQueryService = new TrustedWorkerRecoveryQueryService(
                jobStore,
                attemptStore,
                experimentStore
        );
    }

    @Test
    void findRecoverableQueuedJobsDelegatesWithBounds() {
        Instant olderThan = Instant.now().minusSeconds(120);
        List<RecoverableQueuedJob> expected = List.of(
                new RecoverableQueuedJob(JobId.generate(), ExperimentId.generate(), CandidateId.generate(), olderThan)
        );
        when(jobStore.findRecoverableQueuedJobs(olderThan, 10)).thenReturn(expected);

        List<RecoverableQueuedJob> result = recoveryQueryService.findRecoverableQueuedJobs(olderThan, 10);
        assertThat(result).isEqualTo(expected);
        verify(jobStore).findRecoverableQueuedJobs(olderThan, 10);
    }

    @Test
    void findDueRetriesDelegatesWithBounds() {
        Instant now = Instant.now();
        List<DueRetryJob> expected = List.of(
                new DueRetryJob(JobId.generate(), ExperimentId.generate(), CandidateId.generate(), now)
        );
        when(jobStore.findDueRetries(now, 20)).thenReturn(expected);

        List<DueRetryJob> result = recoveryQueryService.findDueRetries(now, 20);
        assertThat(result).isEqualTo(expected);
        verify(jobStore).findDueRetries(now, 20);
    }

    @Test
    void findStaleRunningAttemptsDelegatesWithBounds() {
        Instant threshold = Instant.now().minusSeconds(300);
        List<StaleRunningAttempt> expected = List.of(
                new StaleRunningAttempt(JobId.generate(), AttemptId.generate(), ExperimentId.generate(), CandidateId.generate(), "w-1", threshold)
        );
        when(attemptStore.findStaleRunningAttempts(threshold, 50)).thenReturn(expected);

        List<StaleRunningAttempt> result = recoveryQueryService.findStaleRunningAttempts(threshold, 50);
        assertThat(result).isEqualTo(expected);
        verify(attemptStore).findStaleRunningAttempts(threshold, 50);
    }

    @Test
    void findStopCompletionCandidatesDelegatesWithBounds() {
        List<StopCandidateExperiment> expected = List.of(
                new StopCandidateExperiment(ExperimentId.generate(), Instant.now())
        );
        when(experimentStore.findStopCompletionCandidates(15)).thenReturn(expected);

        List<StopCandidateExperiment> result = recoveryQueryService.findStopCompletionCandidates(15);
        assertThat(result).isEqualTo(expected);
        verify(experimentStore).findStopCompletionCandidates(15);
    }
}

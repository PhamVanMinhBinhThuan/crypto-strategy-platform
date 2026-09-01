package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import com.cryptostrategy.platform.experiment.api.job.JobType;
import com.cryptostrategy.platform.experiment.api.job.TerminalWorkOutcome;
import com.cryptostrategy.platform.experiment.api.port.out.ExecutionAttemptStore;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import com.cryptostrategy.platform.experiment.api.port.out.JobStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TerminalProgressAndStopCompletionTest {

    private JobStore jobStore;
    private ExperimentStore experimentStore;
    private ExecutionAttemptStore attemptStore;
    private CanonicalFingerprintCalculator fingerprintCalculator;
    private JobApplicationService jobApplicationService;
    private ExperimentApplicationService experimentApplicationService;

    private final UUID ownerUserId = UUID.randomUUID();
    private final ExperimentId experimentId = ExperimentId.generate();
    private final CandidateId candidateId = CandidateId.generate();
    private final JobId jobId = JobId.generate();

    @BeforeEach
    void setUp() {
        jobStore = mock(JobStore.class);
        experimentStore = mock(ExperimentStore.class);
        attemptStore = mock(ExecutionAttemptStore.class);
        fingerprintCalculator = mock(CanonicalFingerprintCalculator.class);

        jobApplicationService = new JobApplicationService(
                jobStore,
                attemptStore,
                experimentStore
        );

        experimentApplicationService = new ExperimentApplicationService(
                experimentStore,
                fingerprintCalculator
        );
    }

    @Test
    void recordTerminalProgressSucceededUpdatesCompletedWorkAndBestScore() {
        Job job = new Job(
                jobId, experimentId, candidateId, JobType.BACKTEST, JobStatus.RUNNING, "corr-1",
                1, 0, 0, null, Instant.now(), Instant.now(), null, null, null, null, Instant.now(), Instant.now()
        );
        when(jobStore.findJobById(ownerUserId, jobId)).thenReturn(Optional.of(job));

        jobApplicationService.recordTerminalProgress(ownerUserId, jobId, TerminalWorkOutcome.SUCCEEDED, new BigDecimal("2.5"));

        verify(jobStore).updateProgress(eq(ownerUserId), eq(jobId), eq(1), eq(0), eq(new BigDecimal("2.5")), any());
    }

    @Test
    void recordTerminalProgressFailedUpdatesFailedWork() {
        Job job = new Job(
                jobId, experimentId, candidateId, JobType.BACKTEST, JobStatus.RUNNING, "corr-1",
                1, 0, 0, null, Instant.now(), Instant.now(), null, null, null, null, Instant.now(), Instant.now()
        );
        when(jobStore.findJobById(ownerUserId, jobId)).thenReturn(Optional.of(job));

        jobApplicationService.recordTerminalProgress(ownerUserId, jobId, TerminalWorkOutcome.FAILED, null);

        verify(jobStore).updateProgress(eq(ownerUserId), eq(jobId), eq(0), eq(1), eq(null), any());
    }

    @Test
    void completeStoppedExperimentTransitionsWhenAllJobsTerminal() {
        Experiment stopRequested = new Experiment(
                experimentId, ownerUserId, "Exp", ExperimentStatus.STOP_REQUESTED,
                null, null, Instant.now(), null, null, null, Instant.now()
        );
        when(experimentStore.findOwnerUserIdByExperimentId(experimentId)).thenReturn(Optional.of(ownerUserId));
        when(experimentStore.findExperimentById(ownerUserId, experimentId)).thenReturn(Optional.of(stopRequested));

        Job terminalJob = new Job(
                jobId, experimentId, candidateId, JobType.BACKTEST, JobStatus.SUCCEEDED, "corr-1",
                1, 1, 0, null, Instant.now(), Instant.now(), Instant.now(), null, null, null, Instant.now(), Instant.now()
        );
        when(experimentStore.listAllJobsByExperimentId(experimentId)).thenReturn(List.of(terminalJob));

        boolean transitioned = experimentApplicationService.completeIfEligible(experimentId);
        assertThat(transitioned).isTrue();
        verify(experimentStore).updateExperimentStatus(eq(ownerUserId), eq(experimentId), eq(ExperimentStatus.STOPPED), any());
    }

    @Test
    void completeStoppedExperimentDoesNotTransitionWhenActiveJobsRemain() {
        Experiment stopRequested = new Experiment(
                experimentId, ownerUserId, "Exp", ExperimentStatus.STOP_REQUESTED,
                null, null, Instant.now(), null, null, null, Instant.now()
        );
        when(experimentStore.findOwnerUserIdByExperimentId(experimentId)).thenReturn(Optional.of(ownerUserId));
        when(experimentStore.findExperimentById(ownerUserId, experimentId)).thenReturn(Optional.of(stopRequested));

        Job runningJob = new Job(
                jobId, experimentId, candidateId, JobType.BACKTEST, JobStatus.RUNNING, "corr-1",
                1, 0, 0, null, Instant.now(), Instant.now(), null, null, null, null, Instant.now(), Instant.now()
        );
        when(experimentStore.listAllJobsByExperimentId(experimentId)).thenReturn(List.of(runningJob));

        boolean transitioned = experimentApplicationService.completeIfEligible(experimentId);
        assertThat(transitioned).isFalse();
    }
}

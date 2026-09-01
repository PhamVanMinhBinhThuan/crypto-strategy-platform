package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.execution.FrozenBacktestExecution;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.AttemptStatus;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.FailureClassification;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import com.cryptostrategy.platform.experiment.api.job.JobType;
import com.cryptostrategy.platform.experiment.api.job.TerminalWorkOutcome;
import com.cryptostrategy.platform.experiment.api.job.WorkerId;
import com.cryptostrategy.platform.experiment.api.port.out.ExecutionAttemptStore;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import com.cryptostrategy.platform.experiment.api.port.out.JobStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrustedWorkerExperimentServiceTest {

    private JobStore jobStore;
    private ExperimentStore experimentStore;
    private ExecutionAttemptStore attemptStore;
    private JobApplicationService jobApplicationService;
    private TrustedWorkerExperimentService trustedService;

    private final UUID ownerUserId = UUID.randomUUID();
    private final ExperimentId experimentId = ExperimentId.generate();
    private final CandidateId candidateId = CandidateId.generate();
    private final JobId jobId = JobId.generate();
    private final AttemptId attemptId = AttemptId.generate();
    private final WorkerId workerId = new WorkerId("worker-1");

    @BeforeEach
    void setUp() {
        jobStore = mock(JobStore.class);
        experimentStore = mock(ExperimentStore.class);
        attemptStore = mock(ExecutionAttemptStore.class);
        jobApplicationService = mock(JobApplicationService.class);

        trustedService = new TrustedWorkerExperimentService(
                jobStore,
                experimentStore,
                attemptStore,
                jobApplicationService
        );

        when(jobStore.findOwnerUserIdByJobId(jobId)).thenReturn(Optional.of(ownerUserId));
        when(experimentStore.findOwnerUserIdByExperimentId(experimentId)).thenReturn(Optional.of(ownerUserId));
    }

    @Test
    void startNextAttemptResolvesOwnerAndDelegates() {
        ExecutionAttempt expected = new ExecutionAttempt(
                attemptId, jobId, candidateId, 1, AttemptStatus.RUNNING,
                "worker-1", Instant.now(), null, null, null, null, false, Instant.now()
        );
        when(jobApplicationService.startNextAttempt(ownerUserId, jobId, "worker-1")).thenReturn(expected);

        ExecutionAttempt actual = trustedService.startNextAttempt(jobId, workerId);
        assertThat(actual).isEqualTo(expected);
        verify(jobApplicationService).startNextAttempt(ownerUserId, jobId, "worker-1");
    }

    @Test
    void finalizeSuccessDelegatesWithResolvedOwner() {
        trustedService.finalizeSuccess(jobId, attemptId);
        verify(jobApplicationService).finalizeSuccess(ownerUserId, jobId, attemptId);
    }

    @Test
    void finalizeFailureDelegatesWithResolvedOwner() {
        Instant nextRetry = Instant.now().plusSeconds(60);
        trustedService.finalizeFailure(jobId, attemptId, "ERR", "msg", FailureClassification.TRANSIENT_NETWORK_ERROR, nextRetry);
        verify(jobApplicationService).finalizeFailure(ownerUserId, jobId, attemptId, "ERR", "msg", FailureClassification.TRANSIENT_NETWORK_ERROR, nextRetry);
    }

    @Test
    void finalizeCancelledDelegatesWithResolvedOwner() {
        trustedService.finalizeCancelled(jobId, attemptId);
        verify(jobApplicationService).finalizeCancelled(ownerUserId, jobId, attemptId);
    }

    @Test
    void recordTerminalProgressDelegatesWithResolvedOwner() {
        trustedService.recordTerminalProgress(jobId, TerminalWorkOutcome.SUCCEEDED, new BigDecimal("1.5"));
        verify(jobApplicationService).recordTerminalProgress(ownerUserId, jobId, TerminalWorkOutcome.SUCCEEDED, new BigDecimal("1.5"));
    }

    @Test
    void isCancelRequestedDelegatesWithResolvedOwner() {
        when(jobApplicationService.isCancelRequested(ownerUserId, jobId)).thenReturn(true);
        assertThat(trustedService.isCancelRequested(jobId)).isTrue();
    }

    @Test
    void requeueDueRetryDelegatesWithResolvedOwner() {
        trustedService.requeueDueRetry(jobId);
        verify(jobApplicationService).requeueRetry(ownerUserId, jobId);
    }
}

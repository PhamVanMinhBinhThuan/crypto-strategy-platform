package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.error.InvalidStateTransitionException;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancellationStateMachineTest {

    private final ExperimentId experimentId = ExperimentId.generate();
    private final CandidateId candidateId = CandidateId.generate();
    private final JobId jobId = JobId.generate();

    @Test
    @DisplayName("Queued Job transitions directly to CANCELLED upon cancellation request")
    void cancelQueuedJob() {
        Job job = Job.createBacktestJob(jobId, experimentId, candidateId, "corr-cancel-1", Instant.now());
        JobAggregate aggregate = new JobAggregate(job, new ArrayList<>());

        aggregate.requestCancel(Instant.now());
        assertThat(aggregate.getJob().status()).isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    @DisplayName("Retry-scheduled Job transitions directly to CANCELLED upon cancellation request")
    void cancelRetryScheduledJob() {
        Job job = new Job(jobId, experimentId, candidateId, com.cryptostrategy.platform.experiment.api.job.JobType.BACKTEST, JobStatus.RETRY_SCHEDULED, "corr", 1, 0, 0, null, Instant.now(), null, null, Instant.now().plusSeconds(60), null, null, Instant.now(), Instant.now());
        JobAggregate aggregate = new JobAggregate(job, new ArrayList<>());

        aggregate.requestCancel(Instant.now());
        assertThat(aggregate.getJob().status()).isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    @DisplayName("Running Job transitions to CANCEL_REQUESTED and then CANCELLED on confirmation")
    void cancelRunningJob() {
        Job job = Job.createBacktestJob(jobId, experimentId, candidateId, "corr-cancel-2", Instant.now());
        JobAggregate aggregate = new JobAggregate(job, new ArrayList<>());

        ExecutionAttempt attempt = aggregate.startAttempt("worker-1", Instant.now());
        aggregate.requestCancel(Instant.now());
        assertThat(aggregate.getJob().status()).isEqualTo(JobStatus.CANCEL_REQUESTED);

        aggregate.confirmCancelled(attempt.attemptId(), Instant.now());
        assertThat(aggregate.getJob().status()).isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    @DisplayName("Cancelling terminal SUCCEEDED or FAILED Job is rejected")
    void cancelTerminalJobRejected() {
        Job succeededJob = new Job(jobId, experimentId, candidateId, com.cryptostrategy.platform.experiment.api.job.JobType.BACKTEST, JobStatus.SUCCEEDED, "corr", 1, 1, 0, null, Instant.now(), Instant.now(), Instant.now(), null, null, null, Instant.now(), Instant.now());
        JobAggregate aggregate = new JobAggregate(succeededJob, new ArrayList<>());

        assertThatThrownBy(() -> aggregate.requestCancel(Instant.now()))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}

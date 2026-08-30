package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.error.InvalidStateTransitionException;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.AttemptStatus;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.FailureClassification;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobAggregateTest {

    private final ExperimentId experimentId = ExperimentId.generate();
    private final CandidateId candidateId = CandidateId.generate();
    private final JobId jobId = JobId.generate();

    @Test
    @DisplayName("Backtest Job starts in QUEUED and transitions through retryable failure to RETRY_SCHEDULED")
    void retryableFailureLifecycle() {
        Job job = Job.createBacktestJob(jobId, experimentId, candidateId, "corr-1", Instant.now());
        JobAggregate aggregate = new JobAggregate(job, new ArrayList<>());

        assertThat(aggregate.getJob().status()).isEqualTo(JobStatus.QUEUED);

        // Start Attempt 1
        Instant start1 = Instant.now();
        ExecutionAttempt attempt1 = aggregate.startAttempt("worker-1", start1);
        assertThat(attempt1.attemptNo()).isEqualTo(1);
        assertThat(attempt1.status()).isEqualTo(AttemptStatus.RUNNING);
        assertThat(aggregate.getJob().status()).isEqualTo(JobStatus.RUNNING);

        // Transient failure on Attempt 1 -> Attempt is FAILED, Job is RETRY_SCHEDULED
        Instant finish1 = Instant.now();
        Instant nextRetry = finish1.plusSeconds(30);
        aggregate.finalizeAttemptFailure(attempt1.attemptId(), "RATE_LIMIT", "Rate limit exceeded", FailureClassification.TRANSIENT, finish1, nextRetry);

        assertThat(aggregate.getAttempts().get(0).status()).isEqualTo(AttemptStatus.FAILED);
        assertThat(aggregate.getAttempts().get(0).retryable()).isTrue();
        assertThat(aggregate.getJob().status()).isEqualTo(JobStatus.RETRY_SCHEDULED);
        assertThat(aggregate.getJob().nextRetryAt()).isEqualTo(nextRetry);

        // Requeue retry -> Job returns to QUEUED
        aggregate.requeueRetry(Instant.now());
        assertThat(aggregate.getJob().status()).isEqualTo(JobStatus.QUEUED);

        // Start Attempt 2
        ExecutionAttempt attempt2 = aggregate.startAttempt("worker-2", Instant.now());
        assertThat(attempt2.attemptNo()).isEqualTo(2);

        // Success on Attempt 2 -> Attempt SUCCEEDED, Job SUCCEEDED
        aggregate.finalizeAttemptSuccess(attempt2.attemptId(), Instant.now());
        assertThat(aggregate.getAttempts().get(1).status()).isEqualTo(AttemptStatus.SUCCEEDED);
        assertThat(aggregate.getJob().status()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(aggregate.getJob().completedWork()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deterministic failure terminates Attempt as FAILED and Job as FAILED without retry")
    void deterministicFailure() {
        Job job = Job.createBacktestJob(jobId, experimentId, candidateId, "corr-2", Instant.now());
        JobAggregate aggregate = new JobAggregate(job, new ArrayList<>());

        ExecutionAttempt attempt = aggregate.startAttempt("worker-1", Instant.now());
        aggregate.finalizeAttemptFailure(attempt.attemptId(), "INVALID_DATA", "Data corrupted", FailureClassification.DETERMINISTIC, Instant.now(), null);

        assertThat(aggregate.getAttempts().get(0).status()).isEqualTo(AttemptStatus.FAILED);
        assertThat(aggregate.getAttempts().get(0).retryable()).isFalse();
        assertThat(aggregate.getJob().status()).isEqualTo(JobStatus.FAILED);
        assertThat(aggregate.getJob().failedWork()).isEqualTo(1);
    }

    @Test
    @DisplayName("Cancellation request moves RUNNING to CANCEL_REQUESTED and then CANCELLED")
    void cancellationFlow() {
        Job job = Job.createBacktestJob(jobId, experimentId, candidateId, "corr-3", Instant.now());
        JobAggregate aggregate = new JobAggregate(job, new ArrayList<>());

        ExecutionAttempt attempt = aggregate.startAttempt("worker-1", Instant.now());
        aggregate.requestCancel(Instant.now());

        assertThat(aggregate.getJob().status()).isEqualTo(JobStatus.CANCEL_REQUESTED);

        aggregate.confirmCancelled(attempt.attemptId(), Instant.now());
        assertThat(aggregate.getJob().status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(aggregate.getAttempts().get(0).status()).isEqualTo(AttemptStatus.CANCELLED);
    }
}

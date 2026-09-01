package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.error.InvalidStateTransitionException;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.AttemptStatus;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.FailureClassification;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class JobAggregate {

    private Job job;
    private final List<ExecutionAttempt> attempts;

    public JobAggregate(Job job, List<ExecutionAttempt> attempts) {
        this.job = Objects.requireNonNull(job, "job cannot be null");
        this.attempts = attempts != null ? new ArrayList<>(attempts) : new ArrayList<>();
    }

    public Job getJob() {
        return job;
    }

    public List<ExecutionAttempt> getAttempts() {
        return Collections.unmodifiableList(attempts);
    }

    public ExecutionAttempt startAttempt(String workerId, Instant startedAt) {
        if (job.status() != JobStatus.QUEUED && job.status() != JobStatus.RETRY_SCHEDULED) {
            throw new InvalidStateTransitionException("Cannot start attempt for job in status: " + job.status());
        }

        int nextAttemptNo = attempts.size() + 1;
        AttemptId attemptId = AttemptId.generate();
        ExecutionAttempt attempt = ExecutionAttempt.start(
                attemptId,
                job.jobId(),
                job.candidateId(),
                nextAttemptNo,
                workerId,
                startedAt
        );

        attempts.add(attempt);
        this.job = new Job(
                job.jobId(),
                job.experimentId(),
                job.candidateId(),
                job.jobType(),
                JobStatus.RUNNING,
                job.correlationId(),
                job.totalWork(),
                job.completedWork(),
                job.failedWork(),
                job.bestScore(),
                job.queuedAt(),
                startedAt,
                null,
                null,
                null,
                null,
                job.createdAt(),
                startedAt
        );

        return attempt;
    }

    public void finalizeAttemptSuccess(AttemptId attemptId, Instant finishedAt) {
        int index = findAttemptIndex(attemptId);
        ExecutionAttempt current = attempts.get(index);
        ExecutionAttempt updated = new ExecutionAttempt(
                current.attemptId(),
                current.jobId(),
                current.candidateId(),
                current.attemptNo(),
                AttemptStatus.SUCCEEDED,
                current.workerId(),
                current.startedAt(),
                finishedAt,
                null,
                null,
                null,
                false,
                current.createdAt()
        );
        attempts.set(index, updated);

        this.job = new Job(
                job.jobId(),
                job.experimentId(),
                job.candidateId(),
                job.jobType(),
                JobStatus.SUCCEEDED,
                job.correlationId(),
                job.totalWork(),
                1,
                0,
                job.bestScore(),
                job.queuedAt(),
                job.startedAt(),
                finishedAt,
                null,
                null,
                null,
                job.createdAt(),
                finishedAt
        );
    }

    public void finalizeAttemptFailure(
            AttemptId attemptId,
            String failureCode,
            String failureMessage,
            FailureClassification classification,
            Instant finishedAt,
            Instant nextRetryAt
    ) {
        int index = findAttemptIndex(attemptId);
        ExecutionAttempt current = attempts.get(index);
        boolean retryable = classification.isRetryable();

        ExecutionAttempt updated = new ExecutionAttempt(
                current.attemptId(),
                current.jobId(),
                current.candidateId(),
                current.attemptNo(),
                AttemptStatus.FAILED,
                current.workerId(),
                current.startedAt(),
                finishedAt,
                retryable ? nextRetryAt : null,
                failureCode,
                failureMessage,
                retryable,
                current.createdAt()
        );
        attempts.set(index, updated);

        JobStatus nextJobStatus = retryable ? JobStatus.RETRY_SCHEDULED : JobStatus.FAILED;
        this.job = new Job(
                job.jobId(),
                job.experimentId(),
                job.candidateId(),
                job.jobType(),
                nextJobStatus,
                job.correlationId(),
                job.totalWork(),
                job.completedWork(),
                retryable ? job.failedWork() : 1,
                job.bestScore(),
                job.queuedAt(),
                job.startedAt(),
                retryable ? null : finishedAt,
                retryable ? nextRetryAt : null,
                failureCode,
                failureMessage,
                job.createdAt(),
                finishedAt
        );
    }

    public void requeueRetry(Instant queuedAt) {
        if (job.status() != JobStatus.RETRY_SCHEDULED) {
            throw new InvalidStateTransitionException("Cannot requeue job not in RETRY_SCHEDULED status: " + job.status());
        }

        this.job = new Job(
                job.jobId(),
                job.experimentId(),
                job.candidateId(),
                job.jobType(),
                JobStatus.QUEUED,
                job.correlationId(),
                job.totalWork(),
                job.completedWork(),
                job.failedWork(),
                job.bestScore(),
                queuedAt,
                null,
                null,
                null,
                null,
                null,
                job.createdAt(),
                queuedAt
        );
    }

    public void requestCancel(Instant requestedAt) {
        if (job.status() == JobStatus.QUEUED || job.status() == JobStatus.RETRY_SCHEDULED) {
            this.job = new Job(
                    job.jobId(),
                    job.experimentId(),
                    job.candidateId(),
                    job.jobType(),
                    JobStatus.CANCELLED,
                    job.correlationId(),
                    job.totalWork(),
                    job.completedWork(),
                    job.failedWork(),
                    job.bestScore(),
                    job.queuedAt(),
                    job.startedAt(),
                    requestedAt,
                    null,
                    null,
                    null,
                    job.createdAt(),
                    requestedAt
            );
        } else if (job.status() == JobStatus.RUNNING) {
            this.job = new Job(
                    job.jobId(),
                    job.experimentId(),
                    job.candidateId(),
                    job.jobType(),
                    JobStatus.CANCEL_REQUESTED,
                    job.correlationId(),
                    job.totalWork(),
                    job.completedWork(),
                    job.failedWork(),
                    job.bestScore(),
                    job.queuedAt(),
                    job.startedAt(),
                    null,
                    null,
                    null,
                    null,
                    job.createdAt(),
                    requestedAt
            );
        } else {
            throw new InvalidStateTransitionException("Cannot cancel job in status: " + job.status());
        }
    }

    public void confirmCancelled(AttemptId attemptId, Instant cancelledAt) {
        if (attemptId != null) {
            int index = findAttemptIndex(attemptId);
            ExecutionAttempt current = attempts.get(index);
            ExecutionAttempt updated = new ExecutionAttempt(
                    current.attemptId(),
                    current.jobId(),
                    current.candidateId(),
                    current.attemptNo(),
                    AttemptStatus.CANCELLED,
                    current.workerId(),
                    current.startedAt(),
                    cancelledAt,
                    null,
                    null,
                    null,
                    false,
                    current.createdAt()
            );
            attempts.set(index, updated);
        }

        this.job = new Job(
                job.jobId(),
                job.experimentId(),
                job.candidateId(),
                job.jobType(),
                JobStatus.CANCELLED,
                job.correlationId(),
                job.totalWork(),
                job.completedWork(),
                job.failedWork(),
                job.bestScore(),
                job.queuedAt(),
                job.startedAt(),
                cancelledAt,
                null,
                null,
                null,
                job.createdAt(),
                cancelledAt
        );
    }

    private int findAttemptIndex(AttemptId attemptId) {
        for (int i = 0; i < attempts.size(); i++) {
            if (attempts.get(i).attemptId().equals(attemptId)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Attempt not found in aggregate: " + attemptId);
    }
}

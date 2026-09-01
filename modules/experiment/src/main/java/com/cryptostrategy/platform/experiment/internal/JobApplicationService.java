package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.error.ExperimentValidationException;
import com.cryptostrategy.platform.experiment.api.error.InvalidStateTransitionException;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.FailureClassification;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.experiment.api.port.in.CancelJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.CreateBacktestJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.CreateSearchJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.FinalizeAttemptUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.RequeueRetryUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.StartNextAttemptUseCase;
import com.cryptostrategy.platform.experiment.api.port.out.ExecutionAttemptStore;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import com.cryptostrategy.platform.experiment.api.port.out.JobStore;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JobApplicationService implements
        CreateBacktestJobUseCase,
        CreateSearchJobUseCase,
        StartNextAttemptUseCase,
        FinalizeAttemptUseCase,
        RequeueRetryUseCase,
        CancelJobUseCase {

    private final JobStore jobStore;
    private final ExecutionAttemptStore attemptStore;
    private final ExperimentStore experimentStore;

    public JobApplicationService(
            JobStore jobStore,
            ExecutionAttemptStore attemptStore,
            ExperimentStore experimentStore
    ) {
        this.jobStore = Objects.requireNonNull(jobStore, "jobStore cannot be null");
        this.attemptStore = Objects.requireNonNull(attemptStore, "attemptStore cannot be null");
        this.experimentStore = Objects.requireNonNull(experimentStore, "experimentStore cannot be null");
    }

    @Override
    public Job createBacktestJob(UUID ownerUserId, ExperimentId experimentId, CandidateId candidateId, String correlationId) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(candidateId, "candidateId cannot be null");
        Objects.requireNonNull(correlationId, "correlationId cannot be null");

        // Verify Experiment exists and is owned by user
        Experiment experiment = experimentStore.findExperimentById(ownerUserId, experimentId)
                .orElseThrow(() -> new ResourceInaccessibleException("Experiment not found or inaccessible"));

        // Verify Candidate exists, belongs to same Experiment, and is accessible
        CandidateDefinition candidate = experimentStore.findCandidateById(ownerUserId, candidateId)
                .orElseThrow(() -> new ResourceInaccessibleException("Candidate not found or inaccessible"));

        if (!candidate.experimentId().equals(experimentId)) {
            throw new ExperimentValidationException("Candidate belongs to experiment " + candidate.experimentId() + ", not " + experimentId);
        }

        // Check if Backtest Job already exists for Candidate
        Optional<Job> existing = jobStore.findBacktestJobByCandidateId(ownerUserId, candidateId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Instant now = Instant.now();
        JobId jobId = JobId.generate();
        Job job = Job.createBacktestJob(jobId, experimentId, candidateId, correlationId, now);

        OutboxEvent outboxEvent = OutboxEvents.jobQueued(job, now);
        jobStore.insertJob(ownerUserId, job, outboxEvent);

        return job;
    }

    @Override
    public Job createSearchJob(UUID ownerUserId, ExperimentId experimentId, String correlationId, int totalGenerations) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(correlationId, "correlationId cannot be null");

        Experiment experiment = experimentStore.findExperimentById(ownerUserId, experimentId)
                .orElseThrow(() -> new ResourceInaccessibleException("Experiment not found or inaccessible"));

        Instant now = Instant.now();
        JobId jobId = JobId.generate();
        Job job = Job.createSearchJob(jobId, experimentId, correlationId, totalGenerations, now);

        OutboxEvent outboxEvent = OutboxEvents.jobQueued(job, now);
        jobStore.insertJob(ownerUserId, job, outboxEvent);

        return job;
    }

    @Override
    public ExecutionAttempt startNextAttempt(UUID ownerUserId, JobId jobId, String workerId) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(workerId, "workerId cannot be null");

        Instant now = Instant.now();
        return attemptStore.startNextAttempt(ownerUserId, jobId, workerId, now);
    }

    @Override
    public void finalizeSuccess(UUID ownerUserId, JobId jobId, AttemptId attemptId) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(attemptId, "attemptId cannot be null");

        Instant now = Instant.now();
        attemptStore.finalizeAttemptSuccess(ownerUserId, jobId, attemptId, now);
    }

    @Override
    public void finalizeFailure(
            UUID ownerUserId,
            JobId jobId,
            AttemptId attemptId,
            String failureCode,
            String failureMessage,
            FailureClassification classification,
            Instant nextRetryAt
    ) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(attemptId, "attemptId cannot be null");
        Objects.requireNonNull(classification, "classification cannot be null");

        Instant now = Instant.now();
        if (classification.isRetryable()) {
            attemptStore.finalizeAttemptRetryableFailure(ownerUserId, jobId, attemptId, failureCode, failureMessage, now, nextRetryAt);
        } else {
            attemptStore.finalizeAttemptTerminalFailure(ownerUserId, jobId, attemptId, failureCode, failureMessage, now);
        }
    }

    @Override
    public void finalizeCancelled(UUID ownerUserId, JobId jobId, AttemptId attemptId) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(attemptId, "attemptId cannot be null");

        Instant now = Instant.now();
        attemptStore.finalizeAttemptCancelled(ownerUserId, jobId, attemptId, now);
    }

    @Override
    public void requeueRetry(UUID ownerUserId, JobId jobId) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");

        Job job = jobStore.findJobById(ownerUserId, jobId)
                .orElseThrow(() -> new ResourceInaccessibleException("Job not found or inaccessible"));

        if (job.status() != JobStatus.RETRY_SCHEDULED) {
            throw new InvalidStateTransitionException("Cannot requeue job in status: " + job.status());
        }

        Instant now = Instant.now();
        Job queuedJob = new Job(
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
                now,
                null,
                null,
                null,
                null,
                null,
                job.createdAt(),
                now
        );

        OutboxEvent outboxEvent = OutboxEvents.jobQueued(queuedJob, now);
        jobStore.requeueRetryWithOutbox(ownerUserId, jobId, outboxEvent, now);
    }

    @Override
    public void cancelJob(UUID ownerUserId, JobId jobId) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");

        Job job = jobStore.findJobById(ownerUserId, jobId)
                .orElseThrow(() -> new ResourceInaccessibleException("Job not found or inaccessible"));

        Instant now = Instant.now();
        if (job.status() == JobStatus.QUEUED) {
            OutboxEvent outboxEvent = OutboxEvents.jobCancelled(job, now);
            jobStore.cancelJobWithOutbox(ownerUserId, jobId, JobStatus.CANCELLED, outboxEvent, now);
        } else if (job.status() == JobStatus.RUNNING) {
            OutboxEvent outboxEvent = OutboxEvents.jobCancelRequested(job, now);
            jobStore.cancelJobWithOutbox(ownerUserId, jobId, JobStatus.CANCEL_REQUESTED, outboxEvent, now);
        } else if (job.status() == JobStatus.RETRY_SCHEDULED) {
            // Local durable cancellation - emits NO Outbox event
            jobStore.cancelJobWithoutOutbox(ownerUserId, jobId, JobStatus.CANCELLED, now);
        } else if (job.status() == JobStatus.CANCEL_REQUESTED || job.status() == JobStatus.CANCELLED) {
            // Already cancelling/cancelled - no-op
        } else {
            throw new InvalidStateTransitionException("Cannot cancel job in terminal status: " + job.status());
        }
    }

    @Override
    public boolean isCancelRequested(UUID ownerUserId, JobId jobId) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");

        return jobStore.findJobById(ownerUserId, jobId)
                .map(j -> j.status() == JobStatus.CANCEL_REQUESTED || j.status() == JobStatus.CANCELLED)
                .orElse(false);
    }
}

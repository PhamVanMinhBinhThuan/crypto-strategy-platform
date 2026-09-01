package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.execution.FrozenBacktestExecution;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.FailureClassification;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobType;
import com.cryptostrategy.platform.experiment.api.job.TerminalWorkOutcome;
import com.cryptostrategy.platform.experiment.api.job.WorkerId;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.out.ExecutionAttemptStore;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import com.cryptostrategy.platform.experiment.api.port.out.JobStore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

public class TrustedWorkerExperimentService implements TrustedWorkerExperimentUseCase {

    private final JobStore jobStore;
    private final ExperimentStore experimentStore;
    private final ExecutionAttemptStore attemptStore;
    private final JobApplicationService jobApplicationService;

    public TrustedWorkerExperimentService(
            JobStore jobStore,
            ExperimentStore experimentStore,
            ExecutionAttemptStore attemptStore,
            JobApplicationService jobApplicationService
    ) {
        this.jobStore = Objects.requireNonNull(jobStore, "jobStore cannot be null");
        this.experimentStore = Objects.requireNonNull(experimentStore, "experimentStore cannot be null");
        this.attemptStore = Objects.requireNonNull(attemptStore, "attemptStore cannot be null");
        this.jobApplicationService = Objects.requireNonNull(jobApplicationService, "jobApplicationService cannot be null");
    }

    @Override
    public ExecutionAttempt startNextAttempt(JobId jobId, WorkerId workerId) {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(workerId, "workerId cannot be null");
        UUID ownerUserId = resolveOwnerByJobId(jobId);
        return jobApplicationService.startNextAttempt(ownerUserId, jobId, workerId.value());
    }

    @Override
    public FrozenBacktestExecution getFrozenExecution(JobId jobId) {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        UUID ownerUserId = resolveOwnerByJobId(jobId);
        Job job = jobStore.findJobById(ownerUserId, jobId)
                .orElseThrow(() -> new ResourceInaccessibleException("Job not found: " + jobId));

        ExperimentId experimentId = job.experimentId();
        Experiment experiment = experimentStore.findExperimentById(ownerUserId, experimentId)
                .orElseThrow(() -> new ResourceInaccessibleException("Experiment not found: " + experimentId));
        ExperimentManifest manifest = experimentStore.findManifestByExperimentId(ownerUserId, experimentId)
                .orElseThrow(() -> new ResourceInaccessibleException("Manifest not found: " + experimentId));
        CandidateDefinition candidate = experimentStore.findCandidateById(ownerUserId, job.candidateId())
                .orElseThrow(() -> new ResourceInaccessibleException("Candidate not found: " + job.candidateId()));

        ExecutionAttempt attempt = attemptStore.listAttemptsByJobId(ownerUserId, jobId).stream()
                .max(Comparator.comparingInt(ExecutionAttempt::attemptNo))
                .orElseThrow(() -> new ResourceInaccessibleException("No attempt found for job: " + jobId));

        validateFrozenExecution(experiment, manifest, candidate, job, attempt);
        return new FrozenBacktestExecution(experiment, manifest, candidate, job, attempt);
    }

    @Override
    public void finalizeSuccess(JobId jobId, AttemptId attemptId) {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(attemptId, "attemptId cannot be null");
        UUID ownerUserId = resolveOwnerByJobId(jobId);
        jobApplicationService.finalizeSuccess(ownerUserId, jobId, attemptId);
    }

    @Override
    public void finalizeFailure(
            JobId jobId,
            AttemptId attemptId,
            String failureCode,
            String failureMessage,
            FailureClassification classification,
            Instant nextRetryAt
    ) {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(attemptId, "attemptId cannot be null");
        UUID ownerUserId = resolveOwnerByJobId(jobId);
        jobApplicationService.finalizeFailure(ownerUserId, jobId, attemptId, failureCode, failureMessage, classification, nextRetryAt);
    }

    @Override
    public void finalizeCancelled(JobId jobId, AttemptId attemptId) {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(attemptId, "attemptId cannot be null");
        UUID ownerUserId = resolveOwnerByJobId(jobId);
        jobApplicationService.finalizeCancelled(ownerUserId, jobId, attemptId);
    }

    @Override
    public boolean isCancelRequested(JobId jobId) {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        UUID ownerUserId = resolveOwnerByJobId(jobId);
        return jobApplicationService.isCancelRequested(ownerUserId, jobId);
    }

    @Override
    public void requeueDueRetry(JobId jobId) {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        UUID ownerUserId = resolveOwnerByJobId(jobId);
        jobApplicationService.requeueRetry(ownerUserId, jobId);
    }

    @Override
    public Job getJob(JobId jobId) {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        UUID ownerUserId = resolveOwnerByJobId(jobId);
        return jobStore.findJobById(ownerUserId, jobId)
                .orElseThrow(() -> new ResourceInaccessibleException("Job not found: " + jobId));
    }

    @Override
    public ExperimentStatus getExperimentStatus(ExperimentId experimentId) {
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        UUID ownerUserId = experimentStore.findOwnerUserIdByExperimentId(experimentId)
                .orElseThrow(() -> new ResourceInaccessibleException("Experiment not found: " + experimentId));
        return experimentStore.findExperimentById(ownerUserId, experimentId)
                .map(Experiment::status)
                .orElseThrow(() -> new ResourceInaccessibleException("Experiment not found: " + experimentId));
    }

    @Override
    public void recordTerminalProgress(JobId jobId, TerminalWorkOutcome outcome, BigDecimal score) {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(outcome, "outcome cannot be null");
        UUID ownerUserId = resolveOwnerByJobId(jobId);
        jobApplicationService.recordTerminalProgress(ownerUserId, jobId, outcome, score);
    }

    private UUID resolveOwnerByJobId(JobId jobId) {
        return jobStore.findOwnerUserIdByJobId(jobId)
                .orElseThrow(() -> new ResourceInaccessibleException("Owner not found for job: " + jobId));
    }

    private static void validateFrozenExecution(
            Experiment experiment,
            ExperimentManifest manifest,
            CandidateDefinition candidate,
            Job job,
            ExecutionAttempt attempt
    ) {
        boolean valid = manifest.fingerprint() != null
                && candidate.experimentId().equals(experiment.experimentId())
                && job.experimentId().equals(experiment.experimentId())
                && candidate.candidateId().equals(job.candidateId())
                && job.jobType() == JobType.BACKTEST
                && attempt.jobId().equals(job.jobId())
                && attempt.candidateId().equals(candidate.candidateId());
        if (!valid) {
            throw new ResourceInaccessibleException("Frozen execution validation failed for job: " + job.jobId());
        }
    }
}

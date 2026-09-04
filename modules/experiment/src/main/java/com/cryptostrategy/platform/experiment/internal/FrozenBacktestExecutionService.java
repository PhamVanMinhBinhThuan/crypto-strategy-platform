package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.execution.FrozenBacktestExecution;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.AttemptStatus;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobType;
import com.cryptostrategy.platform.experiment.api.port.in.GetFrozenBacktestExecutionUseCase;
import com.cryptostrategy.platform.experiment.api.port.out.ExecutionAttemptStore;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import com.cryptostrategy.platform.experiment.api.port.out.JobStore;
import java.util.Objects;
import java.util.UUID;

public final class FrozenBacktestExecutionService implements GetFrozenBacktestExecutionUseCase {
    private final ExperimentStore experiments;
    private final JobStore jobs;
    private final ExecutionAttemptStore attempts;

    public FrozenBacktestExecutionService(
            ExperimentStore experiments,
            JobStore jobs,
            ExecutionAttemptStore attempts
    ) {
        this.experiments = Objects.requireNonNull(experiments);
        this.jobs = Objects.requireNonNull(jobs);
        this.attempts = Objects.requireNonNull(attempts);
    }

    @Override
    public FrozenBacktestExecution getFrozenExecution(
            UUID ownerUserId,
            ExperimentId experimentId,
            CandidateId candidateId,
            JobId jobId,
            AttemptId attemptId
    ) {
        var experiment = experiments.findExperimentById(ownerUserId, experimentId).orElseThrow(this::inaccessible);
        var manifest = experiments.findManifestByExperimentId(ownerUserId, experimentId).orElseThrow(this::inaccessible);
        var candidate = experiments.findCandidateById(ownerUserId, candidateId).orElseThrow(this::inaccessible);
        var job = jobs.findJobById(ownerUserId, jobId).orElseThrow(this::inaccessible);
        var attempt = attempts.listAttemptsByJobId(ownerUserId, jobId).stream()
                .filter(value -> value.attemptId().equals(attemptId))
                .findFirst()
                .orElseThrow(this::inaccessible);

        boolean valid = manifest.fingerprint() != null
                && candidate.experimentId().equals(experimentId)
                && job.experimentId().equals(experimentId)
                && candidate.candidateId().equals(job.candidateId())
                && job.jobType() == JobType.BACKTEST
                && attempt.jobId().equals(jobId)
                && attempt.candidateId().equals(candidateId)
                && attempt.status() == AttemptStatus.RUNNING;
        if (!valid) {
            throw inaccessible();
        }
        return new FrozenBacktestExecution(experiment, manifest, candidate, job, attempt);
    }

    private ResourceInaccessibleException inaccessible() {
        return new ResourceInaccessibleException("Frozen execution not found or inaccessible");
    }
}

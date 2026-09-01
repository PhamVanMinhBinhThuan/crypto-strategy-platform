package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.job.DueRetryJob;
import com.cryptostrategy.platform.experiment.api.job.RecoverableQueuedJob;
import com.cryptostrategy.platform.experiment.api.job.StaleRunningAttempt;
import com.cryptostrategy.platform.experiment.api.job.StopCandidateExperiment;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerRecoveryQueryUseCase;
import com.cryptostrategy.platform.experiment.api.port.out.ExecutionAttemptStore;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import com.cryptostrategy.platform.experiment.api.port.out.JobStore;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class TrustedWorkerRecoveryQueryService implements TrustedWorkerRecoveryQueryUseCase {

    private final JobStore jobStore;
    private final ExecutionAttemptStore attemptStore;
    private final ExperimentStore experimentStore;

    public TrustedWorkerRecoveryQueryService(
            JobStore jobStore,
            ExecutionAttemptStore attemptStore,
            ExperimentStore experimentStore
    ) {
        this.jobStore = Objects.requireNonNull(jobStore, "jobStore cannot be null");
        this.attemptStore = Objects.requireNonNull(attemptStore, "attemptStore cannot be null");
        this.experimentStore = Objects.requireNonNull(experimentStore, "experimentStore cannot be null");
    }

    @Override
    public List<RecoverableQueuedJob> findRecoverableQueuedJobs(Instant olderThan, int limit) {
        Objects.requireNonNull(olderThan, "olderThan cannot be null");
        int boundedLimit = Math.max(1, limit);
        return jobStore.findRecoverableQueuedJobs(olderThan, boundedLimit);
    }

    @Override
    public List<DueRetryJob> findDueRetries(Instant dueAtOrBefore, int limit) {
        Objects.requireNonNull(dueAtOrBefore, "dueAtOrBefore cannot be null");
        int boundedLimit = Math.max(1, limit);
        return jobStore.findDueRetries(dueAtOrBefore, boundedLimit);
    }

    @Override
    public List<StaleRunningAttempt> findStaleRunningAttempts(Instant startedBefore, int limit) {
        Objects.requireNonNull(startedBefore, "startedBefore cannot be null");
        int boundedLimit = Math.max(1, limit);
        return attemptStore.findStaleRunningAttempts(startedBefore, boundedLimit);
    }

    @Override
    public List<StopCandidateExperiment> findStopCompletionCandidates(int limit) {
        int boundedLimit = Math.max(1, limit);
        return experimentStore.findStopCompletionCandidates(boundedLimit);
    }
}

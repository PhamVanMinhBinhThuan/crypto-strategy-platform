package com.cryptostrategy.platform.experiment.api;

import com.cryptostrategy.platform.experiment.api.port.in.CompleteStoppedExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.CancelJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.CreateExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.CreateSearchJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.FreezeExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetFrozenBacktestExecutionUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetStandaloneBacktestUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ReproduceExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.StopExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerRecoveryQueryUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.StartStandaloneBacktestUseCase;
import com.cryptostrategy.platform.experiment.api.port.out.ExecutionAttemptStore;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import com.cryptostrategy.platform.experiment.api.port.out.JobStore;
import com.cryptostrategy.platform.experiment.api.port.out.StandaloneBacktestStore;
import com.cryptostrategy.platform.experiment.internal.CanonicalFingerprintCalculator;
import com.cryptostrategy.platform.experiment.internal.ExperimentApplicationService;
import com.cryptostrategy.platform.experiment.internal.FrozenBacktestExecutionService;
import com.cryptostrategy.platform.experiment.internal.JobApplicationService;
import com.cryptostrategy.platform.experiment.internal.StandaloneBacktestService;
import com.cryptostrategy.platform.experiment.internal.TrustedWorkerExperimentService;
import com.cryptostrategy.platform.experiment.internal.TrustedWorkerRecoveryQueryService;

public final class ExperimentModuleFactory {
    private ExperimentModuleFactory() {}

    public static StartStandaloneBacktestUseCase startStandaloneBacktestUseCase(
            StandaloneBacktestStore store,
            java.time.Clock clock
    ) {
        return new StandaloneBacktestService(
                store, new CanonicalFingerprintCalculator(), clock);
    }

    public static GetStandaloneBacktestUseCase getStandaloneBacktestUseCase(
            StandaloneBacktestStore store,
            java.time.Clock clock) {
        return new StandaloneBacktestService(
                store, new CanonicalFingerprintCalculator(), clock);
    }

    /** Public application capabilities used by request/response adapters. */
    public static ApplicationComponents applicationComponents(
            ExperimentStore experimentStore,
            JobStore jobStore,
            ExecutionAttemptStore attemptStore) {
        ExperimentApplicationService experiments = new ExperimentApplicationService(
                experimentStore, new CanonicalFingerprintCalculator());
        JobApplicationService jobs = new JobApplicationService(
                jobStore, attemptStore, experimentStore);
        return new ApplicationComponents(
                experiments,
                experiments,
                experiments,
                experiments,
                experiments,
                experiments,
                jobs,
                jobs,
                jobs);
    }

    public record ApplicationComponents(
            CreateExperimentUseCase createExperiment,
            FreezeExperimentUseCase freezeExperiment,
            GetExperimentUseCase getExperiment,
            StopExperimentUseCase stopExperiment,
            ReproduceExperimentUseCase reproduceExperiment,
            ListCandidatesUseCase candidates,
            CreateSearchJobUseCase createSearchJob,
            GetJobUseCase getJob,
            CancelJobUseCase cancelJob) {}

    public static TrustedWorkerExperimentUseCase trustedWorkerExperimentUseCase(
            JobStore jobStore,
            ExperimentStore experimentStore,
            ExecutionAttemptStore attemptStore
    ) {
        JobApplicationService jobApp = new JobApplicationService(jobStore, attemptStore, experimentStore);
        return new TrustedWorkerExperimentService(jobStore, experimentStore, attemptStore, jobApp);
    }

    public static TrustedWorkerRecoveryQueryUseCase trustedWorkerRecoveryQueryUseCase(
            JobStore jobStore,
            ExecutionAttemptStore attemptStore,
            ExperimentStore experimentStore
    ) {
        return new TrustedWorkerRecoveryQueryService(jobStore, attemptStore, experimentStore);
    }

    public static CompleteStoppedExperimentUseCase completeStoppedExperimentUseCase(ExperimentStore experimentStore) {
        return new ExperimentApplicationService(experimentStore, new CanonicalFingerprintCalculator());
    }

    public static GetFrozenBacktestExecutionUseCase frozenBacktestExecutionUseCase(
            ExperimentStore experimentStore,
            JobStore jobStore,
            ExecutionAttemptStore attemptStore
    ) {
        return new FrozenBacktestExecutionService(experimentStore, jobStore, attemptStore);
    }
}

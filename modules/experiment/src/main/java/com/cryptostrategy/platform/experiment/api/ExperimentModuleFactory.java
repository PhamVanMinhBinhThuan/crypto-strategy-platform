package com.cryptostrategy.platform.experiment.api;

import com.cryptostrategy.platform.experiment.api.port.in.CompleteStoppedExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetFrozenBacktestExecutionUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerRecoveryQueryUseCase;
import com.cryptostrategy.platform.experiment.api.port.out.ExecutionAttemptStore;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import com.cryptostrategy.platform.experiment.api.port.out.JobStore;
import com.cryptostrategy.platform.experiment.internal.CanonicalFingerprintCalculator;
import com.cryptostrategy.platform.experiment.internal.ExperimentApplicationService;
import com.cryptostrategy.platform.experiment.internal.FrozenBacktestExecutionService;
import com.cryptostrategy.platform.experiment.internal.JobApplicationService;
import com.cryptostrategy.platform.experiment.internal.TrustedWorkerExperimentService;
import com.cryptostrategy.platform.experiment.internal.TrustedWorkerRecoveryQueryService;

public final class ExperimentModuleFactory {
    private ExperimentModuleFactory() {}

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

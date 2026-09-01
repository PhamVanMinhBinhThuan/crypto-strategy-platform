package com.cryptostrategy.platform.execution.api;

import com.cryptostrategy.platform.backtesting.api.port.in.CommitPreparedBacktestUseCase;
import com.cryptostrategy.platform.backtesting.api.port.out.FrozenStrategyResolver;
import com.cryptostrategy.platform.combination.api.CompositeStrategyMaterializer;
import com.cryptostrategy.platform.evaluation.api.port.in.EvaluateBacktestUseCase;
import com.cryptostrategy.platform.execution.api.port.in.CompleteBacktestAttemptUseCase;
import com.cryptostrategy.platform.execution.internal.CompleteBacktestAttemptService;
import com.cryptostrategy.platform.execution.internal.RegistryFrozenStrategyResolver;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyFingerprintCalculator;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import org.springframework.transaction.support.TransactionTemplate;

public final class ExperimentExecutionModuleFactory {
    private ExperimentExecutionModuleFactory() {}

    public static FrozenStrategyResolver strategyResolver(
            StrategyRegistry registry,
            StrategyFingerprintCalculator fingerprints,
            CompositeStrategyMaterializer composites
    ) {
        return new RegistryFrozenStrategyResolver(registry, fingerprints, composites);
    }

    public static CompleteBacktestAttemptUseCase completeBacktestAttemptUseCase(
            TrustedWorkerExperimentUseCase experimentUseCase,
            CommitPreparedBacktestUseCase commitBacktestUseCase,
            EvaluateBacktestUseCase evaluateBacktestUseCase,
            TransactionTemplate transactionTemplate
    ) {
        return new CompleteBacktestAttemptService(
                experimentUseCase,
                commitBacktestUseCase,
                evaluateBacktestUseCase,
                transactionTemplate
        );
    }
}

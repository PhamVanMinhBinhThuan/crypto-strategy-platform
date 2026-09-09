package com.cryptostrategy.platform.execution.api;

import com.cryptostrategy.platform.execution.api.port.in.*;
import com.cryptostrategy.platform.execution.api.port.out.*;
import com.cryptostrategy.platform.execution.internal.*;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import com.cryptostrategy.platform.search.api.port.in.SearchGenerationUseCase;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyFingerprintCalculator;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.cryptostrategy.platform.strategy.api.port.in.ResolveStrategySnapshotUseCase;
import com.cryptostrategy.platform.combination.api.CompositeStrategyMaterializer;
import com.cryptostrategy.platform.backtesting.api.port.out.FrozenStrategyResolver;
import com.cryptostrategy.platform.backtesting.api.port.in.CommitPreparedBacktestUseCase;
import com.cryptostrategy.platform.evaluation.api.port.in.EvaluateBacktestUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.transaction.support.TransactionTemplate;

/** Published composition boundary; hosts never instantiate execution internals. */
public final class ExperimentExecutionModuleFactory {
    private ExperimentExecutionModuleFactory() {}
    public static ListSearchGeneratorsUseCase searchGenerators() {
        return () -> {
            var descriptor = com.cryptostrategy.platform.search.api.SearchModuleFactory
                    .baselineDefinition(0L).descriptor();
            return java.util.List.of(new ListSearchGeneratorsUseCase.GeneratorDescriptor(
                    new RequestedGeneratorId(descriptor.generatorId().value()),
                    descriptor.generatorVersion().toString(), "Random Search",
                    descriptor.stateContractVersion(), descriptor.descriptorFingerprint()));
        };
    }
    public static StartSearchExperimentUseCase start(SearchExperimentTransactionGateway gateway) { return new SearchExperimentOrchestrationService(gateway); }
    public static StartSearchReproductionUseCase reproduce(SearchReproductionGateway gateway) { return new SearchReproductionApplicationService(gateway); }
    public static SearchStartCommandFactory startCommands(GetDatasetUseCase datasets, StrategyRegistry strategies,
            ResolveStrategySnapshotUseCase userStrategies, StrategyFingerprintCalculator fingerprints,
            ObjectMapper json, String version, String commit, Clock clock) {
        return new SearchStartCommandFactoryService(datasets, strategies, userStrategies, fingerprints,
                json, version, commit, clock);
    }
    public static TrustedSearchCoordinationUseCase trustedCoordination(TrustedSearchCoordinationGateway gateway, Clock clock) { return new TrustedSearchCoordinationService(gateway, clock); }
    public static SearchCandidateAllocationUseCase allocation(SearchRunStore runs, SearchGenerationUseCase generation,
            SearchAllocationContextGateway contexts, SearchExperimentTransactionGateway transactions, Clock clock,
            ObjectMapper json) {
        return new SearchCandidateAllocationService(runs, generation, contexts, transactions, clock, json);
    }
    public static SearchReproductionVerificationUseCase reproductionVerification(SearchReproductionVerificationGateway gateway,
            ExecutionEvidenceReader evidence, Clock clock) { return new SearchReproductionVerificationCoordinator(gateway, evidence, clock)::reconcile; }

    public static FrozenStrategyResolver strategyResolver(StrategyRegistry registry,
            StrategyFingerprintCalculator fingerprints, CompositeStrategyMaterializer materializer) {
        return new RegistryFrozenStrategyResolver(registry, fingerprints, materializer);
    }

    public static CompleteBacktestAttemptUseCase completeBacktestAttemptUseCase(
            TrustedWorkerExperimentUseCase experiments, CommitPreparedBacktestUseCase backtests,
            EvaluateBacktestUseCase evaluations, TransactionTemplate transactions) {
        return new CompleteBacktestAttemptService(experiments, backtests, evaluations, transactions);
    }
}

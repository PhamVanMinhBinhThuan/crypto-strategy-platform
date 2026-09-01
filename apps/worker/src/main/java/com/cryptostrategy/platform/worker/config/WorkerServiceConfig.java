package com.cryptostrategy.platform.worker.config;

import com.cryptostrategy.platform.backtesting.api.BacktestingModuleFactory;
import com.cryptostrategy.platform.backtesting.api.port.in.CommitPreparedBacktestUseCase;
import com.cryptostrategy.platform.backtesting.api.port.in.PrepareBacktestUseCase;
import com.cryptostrategy.platform.backtesting.api.port.in.RunBacktestUseCase;
import com.cryptostrategy.platform.backtesting.api.port.out.BacktestResultStore;
import com.cryptostrategy.platform.backtesting.api.port.out.FrozenStrategyResolver;
import com.cryptostrategy.platform.combination.api.CombinationModuleFactory;
import com.cryptostrategy.platform.combination.api.CombinationPolicies;
import com.cryptostrategy.platform.combination.api.CompositeStrategyMaterializer;
import com.cryptostrategy.platform.evaluation.api.EvaluationModuleFactory;
import com.cryptostrategy.platform.evaluation.api.port.in.EvaluateBacktestUseCase;
import com.cryptostrategy.platform.evaluation.api.port.out.EvaluationResultStore;
import com.cryptostrategy.platform.execution.api.ExperimentExecutionModuleFactory;
import com.cryptostrategy.platform.execution.api.port.in.CompleteBacktestAttemptUseCase;
import com.cryptostrategy.platform.experiment.api.ExperimentModuleFactory;
import com.cryptostrategy.platform.experiment.api.port.in.CompleteStoppedExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetFrozenBacktestExecutionUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerRecoveryQueryUseCase;
import com.cryptostrategy.platform.experiment.api.port.out.ExecutionAttemptStore;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import com.cryptostrategy.platform.experiment.api.port.out.JobStore;
import com.cryptostrategy.platform.leaderboard.api.LeaderboardModuleFactory;
import com.cryptostrategy.platform.leaderboard.api.port.in.LeaderboardReconciliationUseCase;
import com.cryptostrategy.platform.leaderboard.api.port.in.ProjectLeaderboardUseCase;
import com.cryptostrategy.platform.leaderboard.api.port.out.LeaderboardStore;
import com.cryptostrategy.platform.marketdata.api.MarketDataModuleFactory;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetCandleReader;
import com.cryptostrategy.platform.persistence.api.BacktestingPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.EvaluationPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.ExperimentPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.LeaderboardPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.MarketDataPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.WorkerPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.worker.OutboxPublicationPort;
import com.cryptostrategy.platform.persistence.api.worker.ProcessedMessageStore;
import com.cryptostrategy.platform.strategies.api.StrategyPlugins;
import com.cryptostrategy.platform.strategy.api.StrategyModuleFactory;
import com.cryptostrategy.platform.strategy.api.StrategyPlugin;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyFingerprintCalculator;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.List;

@Configuration
public class WorkerServiceConfig {

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    public WorkerPersistenceFactory workerPersistenceFactory(DataSource dataSource) {
        return new WorkerPersistenceFactory(dataSource);
    }

    @Bean
    public OutboxPublicationPort outboxPublicationPort(WorkerPersistenceFactory factory) {
        return factory.createOutboxPublicationPort();
    }

    @Bean
    public ProcessedMessageStore processedMessageStore(WorkerPersistenceFactory factory) {
        return factory.createProcessedMessageStore();
    }

    @Bean
    public ExperimentPersistenceFactory experimentPersistenceFactory(DataSource dataSource) {
        return new ExperimentPersistenceFactory(dataSource);
    }

    @Bean
    public JobStore jobStore(ExperimentPersistenceFactory factory) {
        return factory.createJobStore();
    }

    @Bean
    public ExecutionAttemptStore executionAttemptStore(ExperimentPersistenceFactory factory) {
        return factory.createExecutionAttemptStore();
    }

    @Bean
    public ExperimentStore experimentStore(ExperimentPersistenceFactory factory) {
        return factory.createExperimentStore();
    }

    @Bean
    public BacktestingPersistenceFactory backtestingPersistenceFactory(DataSource dataSource) {
        return new BacktestingPersistenceFactory(dataSource);
    }

    @Bean
    public BacktestResultStore backtestResultStore(BacktestingPersistenceFactory factory) {
        return factory.createResultStore();
    }

    @Bean
    public EvaluationPersistenceFactory evaluationPersistenceFactory(DataSource dataSource) {
        return new EvaluationPersistenceFactory(dataSource);
    }

    @Bean
    public EvaluationResultStore evaluationResultStore(EvaluationPersistenceFactory factory) {
        return factory.createStore();
    }

    @Bean
    public LeaderboardPersistenceFactory leaderboardPersistenceFactory(DataSource dataSource) {
        return new LeaderboardPersistenceFactory(dataSource);
    }

    @Bean
    public LeaderboardStore leaderboardStore(LeaderboardPersistenceFactory factory) {
        return factory.createStore();
    }

    @Bean
    public TrustedWorkerExperimentUseCase trustedWorkerExperimentUseCase(
            JobStore jobStore,
            ExperimentStore experimentStore,
            ExecutionAttemptStore attemptStore
    ) {
        return ExperimentModuleFactory.trustedWorkerExperimentUseCase(jobStore, experimentStore, attemptStore);
    }

    @Bean
    public TrustedWorkerRecoveryQueryUseCase trustedWorkerRecoveryQueryUseCase(
            JobStore jobStore,
            ExecutionAttemptStore attemptStore,
            ExperimentStore experimentStore
    ) {
        return ExperimentModuleFactory.trustedWorkerRecoveryQueryUseCase(jobStore, attemptStore, experimentStore);
    }

    @Bean
    public CompleteStoppedExperimentUseCase completeStoppedExperimentUseCase(ExperimentStore experimentStore) {
        return ExperimentModuleFactory.completeStoppedExperimentUseCase(experimentStore);
    }

    @Bean
    public GetFrozenBacktestExecutionUseCase getFrozenBacktestExecutionUseCase(
            ExperimentStore experimentStore,
            JobStore jobStore,
            ExecutionAttemptStore attemptStore
    ) {
        return ExperimentModuleFactory.frozenBacktestExecutionUseCase(experimentStore, jobStore, attemptStore);
    }

    @Bean
    public EvaluateBacktestUseCase evaluateBacktestUseCase(EvaluationResultStore evaluationResultStore) {
        return EvaluationModuleFactory.evaluateBacktestUseCase(evaluationResultStore);
    }

    @Bean
    public ProjectLeaderboardUseCase projectLeaderboardUseCase(LeaderboardStore leaderboardStore) {
        return LeaderboardModuleFactory.projectLeaderboardUseCase(leaderboardStore);
    }

    @Bean
    public LeaderboardReconciliationUseCase leaderboardReconciliationUseCase(
            LeaderboardStore leaderboardStore,
            ProjectLeaderboardUseCase projectLeaderboardUseCase
    ) {
        return LeaderboardModuleFactory.leaderboardReconciliationUseCase(leaderboardStore, projectLeaderboardUseCase);
    }

    @Bean
    public MarketDataPersistenceFactory.Components marketDataPersistenceComponents(DataSource dataSource) {
        return MarketDataPersistenceFactory.create(dataSource);
    }

    @Bean
    public MarketDataModuleFactory.Components marketDataModuleComponents(
            MarketDataPersistenceFactory.Components persistenceComponents
    ) {
        return MarketDataModuleFactory.create(
                MarketDataModuleFactory.fixtureProvider(List.of()),
                persistenceComponents.candles(),
                persistenceComponents.datasets(),
                persistenceComponents.reader(),
                Clock.systemUTC()
        );
    }

    @Bean
    public DatasetCandleReader datasetCandleReader(MarketDataPersistenceFactory.Components persistenceComponents) {
        return persistenceComponents.reader();
    }

    @Bean
    public StrategyRegistry strategyRegistry() {
        return StrategyModuleFactory.registry(StrategyPlugins.trusted());
    }

    @Bean
    public StrategyFingerprintCalculator strategyFingerprintCalculator() {
        return StrategyModuleFactory.fingerprints();
    }

    @Bean
    public CompositeStrategyMaterializer compositeStrategyMaterializer() {
        return CombinationModuleFactory.materializer(CombinationPolicies.supported());
    }

    @Bean
    public FrozenStrategyResolver frozenStrategyResolver(
            StrategyRegistry strategyRegistry,
            StrategyFingerprintCalculator fingerprintCalculator,
            CompositeStrategyMaterializer materializer
    ) {
        return ExperimentExecutionModuleFactory.strategyResolver(strategyRegistry, fingerprintCalculator, materializer);
    }

    @Bean
    public PrepareBacktestUseCase prepareBacktestUseCase(
            GetFrozenBacktestExecutionUseCase frozenExecutionUseCase,
            MarketDataModuleFactory.Components marketDataComponents,
            MarketDataPersistenceFactory.Components marketDataPersistence,
            FrozenStrategyResolver strategyResolver,
            BacktestResultStore backtestResultStore
    ) {
        return BacktestingModuleFactory.runBacktestService(
                frozenExecutionUseCase,
                marketDataComponents.getDataset(),
                marketDataComponents.verifyDataset(),
                marketDataPersistence.reader(),
                strategyResolver,
                backtestResultStore
        );
    }

    @Bean
    public CompleteBacktestAttemptUseCase completeBacktestAttemptUseCase(
            TrustedWorkerExperimentUseCase experimentUseCase,
            PrepareBacktestUseCase prepareBacktestUseCase,
            EvaluateBacktestUseCase evaluateBacktestUseCase,
            TransactionTemplate transactionTemplate
    ) {
        return ExperimentExecutionModuleFactory.completeBacktestAttemptUseCase(
                experimentUseCase,
                (CommitPreparedBacktestUseCase) prepareBacktestUseCase,
                evaluateBacktestUseCase,
                transactionTemplate
        );
    }
}

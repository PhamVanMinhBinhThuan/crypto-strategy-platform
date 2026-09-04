package com.cryptostrategy.platform.api.config;

import com.cryptostrategy.platform.experiment.api.ExperimentModuleFactory;
import com.cryptostrategy.platform.experiment.api.port.in.CancelJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.StopExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.out.ExecutionAttemptStore;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import com.cryptostrategy.platform.experiment.api.port.out.JobStore;
import com.cryptostrategy.platform.persistence.api.ExperimentPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.SearchPersistenceFactory;
import javax.sql.DataSource;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchExperimentUseCase;
import com.cryptostrategy.platform.execution.api.port.out.SearchExperimentTransactionGateway;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchReproductionUseCase;
import com.cryptostrategy.platform.execution.api.port.in.GetSearchReproductionVerificationUseCase;
import com.cryptostrategy.platform.execution.api.port.out.SearchReproductionGateway;
import com.cryptostrategy.platform.execution.api.port.in.SearchStartCommandFactory;
import com.cryptostrategy.platform.execution.api.ExperimentExecutionModuleFactory;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyFingerprintCalculator;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.cryptostrategy.platform.strategy.api.port.in.ResolveStrategySnapshotUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ExperimentApiConfiguration {
    @Bean("searchApiClock")
    Clock searchApiClock() {
        return Clock.systemUTC();
    }

    @Bean
    SearchStartCommandFactory searchStartCommandFactory(
            GetDatasetUseCase datasets,
            StrategyRegistry strategies,
            ResolveStrategySnapshotUseCase userStrategies,
            StrategyFingerprintCalculator fingerprints,
            ObjectMapper objectMapper,
            @Value("${platform.build.version:development}") String softwareVersion,
            @Value("${platform.build.git-commit:unknown}") String gitCommit) {
        return ExperimentExecutionModuleFactory.startCommands(
                datasets, strategies, userStrategies, fingerprints, objectMapper, softwareVersion,
                gitCommit, Clock.systemUTC());
    }

    @Bean
    SearchPersistenceFactory.ExperimentTransactions searchExperimentTransactions(DataSource dataSource) {
        return new SearchPersistenceFactory(dataSource).createExperimentTransactions();
    }

    @Bean
    SearchExperimentTransactionGateway searchExperimentTransactionGateway(
            SearchPersistenceFactory.ExperimentTransactions transactions) {
        return transactions.start();
    }

    @Bean
    SearchReproductionGateway searchReproductionGateway(
            SearchPersistenceFactory.ExperimentTransactions transactions) {
        return transactions.reproduction();
    }

    @Bean
    StartSearchExperimentUseCase startSearchExperimentUseCase(
            SearchExperimentTransactionGateway transactions) {
        return ExperimentExecutionModuleFactory.start(transactions);
    }

    @Bean
    StartSearchReproductionUseCase startSearchReproductionUseCase(
            @Qualifier("searchReproductionGateway") SearchReproductionGateway gateway) {
        return ExperimentExecutionModuleFactory.reproduce(gateway);
    }

    @Bean
    GetSearchReproductionVerificationUseCase getSearchReproductionVerificationUseCase(
            DataSource dataSource, ObjectMapper objectMapper) {
        return new SearchPersistenceFactory(dataSource)
                .createReproductionVerificationQuery(objectMapper);
    }

    @Bean
    ExperimentStore experimentStore(DataSource dataSource) {
        return new ExperimentPersistenceFactory(dataSource).createExperimentStore();
    }

    @Bean
    JobStore jobStore(DataSource dataSource) {
        return new ExperimentPersistenceFactory(dataSource).createJobStore();
    }

    @Bean
    ExecutionAttemptStore executionAttemptStore(DataSource dataSource) {
        return new ExperimentPersistenceFactory(dataSource).createExecutionAttemptStore();
    }

    @Bean
    ExperimentModuleFactory.ApplicationComponents experimentApplicationComponents(
            ExperimentStore experiments,
            JobStore jobs,
            ExecutionAttemptStore attempts) {
        return ExperimentModuleFactory.applicationComponents(experiments, jobs, attempts);
    }

    @Bean
    GetExperimentUseCase getExperimentUseCase(
            ExperimentModuleFactory.ApplicationComponents components) {
        return new GetExperimentUseCase() {
            @Override
            public java.util.Optional<com.cryptostrategy.platform.experiment.api.Experiment> getExperiment(
                    java.util.UUID ownerUserId,
                    com.cryptostrategy.platform.experiment.api.ExperimentId experimentId) {
                return components.getExperiment().getExperiment(ownerUserId, experimentId);
            }

            @Override
            public java.util.Optional<com.cryptostrategy.platform.experiment.api.ExperimentManifest> getManifest(
                    java.util.UUID ownerUserId,
                    com.cryptostrategy.platform.experiment.api.ExperimentId experimentId) {
                return components.getExperiment().getManifest(ownerUserId, experimentId);
            }
        };
    }

    @Bean
    StopExperimentUseCase stopExperimentUseCase(
            ExperimentModuleFactory.ApplicationComponents components) {
        return components.stopExperiment()::stopExperiment;
    }

    @Bean
    ListCandidatesUseCase listCandidatesUseCase(
            ExperimentModuleFactory.ApplicationComponents components) {
        return new ListCandidatesUseCase() {
            @Override
            public java.util.List<com.cryptostrategy.platform.experiment.api.CandidateDefinition> listCandidates(
                    java.util.UUID ownerUserId,
                    com.cryptostrategy.platform.experiment.api.ExperimentId experimentId) {
                return components.candidates().listCandidates(ownerUserId, experimentId);
            }

            @Override
            public java.util.List<com.cryptostrategy.platform.experiment.api.CandidateDefinition> listCandidates(
                    java.util.UUID ownerUserId,
                    com.cryptostrategy.platform.experiment.api.ExperimentId experimentId,
                    int afterGenerationIndex,
                    String afterCandidateId,
                    int limit) {
                return components.candidates().listCandidates(
                        ownerUserId,
                        experimentId,
                        afterGenerationIndex,
                        afterCandidateId,
                        limit);
            }

            @Override
            public java.util.Optional<com.cryptostrategy.platform.experiment.api.CandidateDefinition> getCandidate(
                    java.util.UUID ownerUserId,
                    com.cryptostrategy.platform.experiment.api.ExperimentId experimentId,
                    com.cryptostrategy.platform.experiment.api.CandidateId candidateId) {
                return components.candidates().getCandidate(ownerUserId, experimentId, candidateId);
            }
        };
    }

    @Bean
    GetJobUseCase getJobUseCase(
            ExperimentModuleFactory.ApplicationComponents components) {
        return new GetJobUseCase() {
            @Override
            public java.util.Optional<com.cryptostrategy.platform.experiment.api.job.Job> getJob(
                    java.util.UUID ownerUserId,
                    com.cryptostrategy.platform.experiment.api.job.JobId jobId) {
                return components.getJob().getJob(ownerUserId, jobId);
            }

            @Override
            public java.util.List<com.cryptostrategy.platform.experiment.api.job.Job> listJobs(
                    java.util.UUID ownerUserId,
                    com.cryptostrategy.platform.experiment.api.ExperimentId experimentId) {
                return components.getJob().listJobs(ownerUserId, experimentId);
            }
        };
    }

    @Bean
    CancelJobUseCase cancelJobUseCase(
            ExperimentModuleFactory.ApplicationComponents components) {
        return new CancelJobUseCase() {
            @Override
            public void cancelJob(
                    java.util.UUID ownerUserId,
                    com.cryptostrategy.platform.experiment.api.job.JobId jobId) {
                components.cancelJob().cancelJob(ownerUserId, jobId);
            }

            @Override
            public boolean isCancelRequested(
                    java.util.UUID ownerUserId,
                    com.cryptostrategy.platform.experiment.api.job.JobId jobId) {
                return components.cancelJob().isCancelRequested(ownerUserId, jobId);
            }
        };
    }
}

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
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ExperimentApiConfiguration {
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

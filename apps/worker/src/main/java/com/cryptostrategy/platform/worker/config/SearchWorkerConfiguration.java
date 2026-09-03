package com.cryptostrategy.platform.worker.config;

import com.cryptostrategy.platform.execution.api.port.in.SearchCandidateAllocationUseCase;
import com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase;
import com.cryptostrategy.platform.execution.api.port.out.TrustedSearchCoordinationGateway;
import com.cryptostrategy.platform.execution.api.port.out.SearchAllocationContextGateway;
import com.cryptostrategy.platform.execution.api.port.out.SearchExperimentTransactionGateway;
import com.cryptostrategy.platform.execution.api.port.out.SearchReproductionVerificationGateway;
import com.cryptostrategy.platform.execution.api.port.out.ExecutionEvidenceReader;
import com.cryptostrategy.platform.execution.api.port.in.SearchReproductionVerificationUseCase;
import com.cryptostrategy.platform.execution.api.ExperimentExecutionModuleFactory;
import com.cryptostrategy.platform.persistence.api.ExperimentExecutionPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.SearchPersistenceFactory;
import com.cryptostrategy.platform.search.api.SearchModuleFactory;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamMessageReader;
import com.cryptostrategy.platform.worker.search.consumer.SearchRequestConsumer;
import com.cryptostrategy.platform.worker.search.consumer.SearchCompletionConsumer;
import com.cryptostrategy.platform.worker.search.coordination.SearchCoordinator;
import com.cryptostrategy.platform.worker.search.reconciliation.SearchReconciler;
import com.cryptostrategy.platform.worker.search.coordination.SearchFailureHandler;
import com.cryptostrategy.platform.worker.search.coordination.SearchObservability;
import com.cryptostrategy.platform.worker.infra.redis.DeadLetterPublisher;
import com.cryptostrategy.platform.worker.infra.redis.LifecycleNotificationPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root Worker cho Search; business decisions vẫn qua published execution port. */
@Configuration(proxyBeanMethods = false)
public class SearchWorkerConfiguration {
    @Bean
    SearchPersistenceFactory searchPersistenceFactory(DataSource dataSource) {
        return new SearchPersistenceFactory(dataSource);
    }

    @Bean
    SearchRunStore searchRunStore(SearchPersistenceFactory persistence) {
        return persistence.createSearchRunStore();
    }

    @Bean
    TrustedSearchCoordinationGateway trustedSearchCoordinationGateway(SearchPersistenceFactory persistence) {
        return persistence.createTrustedCoordinationGateway();
    }

    @Bean
    TrustedSearchCoordinationUseCase trustedSearchCoordinationUseCase(
            TrustedSearchCoordinationGateway gateway) {
        return ExperimentExecutionModuleFactory.trustedCoordination(gateway, Clock.systemUTC());
    }

    @Bean
    SearchModuleFactory.Components searchComponents(SearchRunStore runs) {
        return SearchModuleFactory.baseline(runs);
    }

    @Bean
    SearchAllocationContextGateway searchAllocationContextGateway(SearchPersistenceFactory persistence) {
        return persistence.createAllocationContextGateway();
    }

    @Bean
    SearchExperimentTransactionGateway searchExperimentTransactionGateway(SearchPersistenceFactory persistence) {
        return persistence.createExperimentTransactionGateway();
    }

    @Bean
    SearchReproductionVerificationGateway searchReproductionVerificationGateway(
            SearchPersistenceFactory persistence) {
        return persistence.createReproductionVerificationGateway();
    }

    @Bean
    ExecutionEvidenceReader searchExecutionEvidenceReader(DataSource dataSource, ObjectMapper mapper) {
        return new ExperimentExecutionPersistenceFactory(dataSource, mapper).createEvidenceReader();
    }

    @Bean
    SearchReproductionVerificationUseCase searchReproductionVerificationCoordinator(
            SearchReproductionVerificationGateway gateway, ExecutionEvidenceReader evidence) {
        return ExperimentExecutionModuleFactory.reproductionVerification(gateway, evidence, Clock.systemUTC());
    }

    @Bean
    SearchCandidateAllocationUseCase searchCandidateAllocationUseCase(
            SearchRunStore runs,
            SearchModuleFactory.Components search,
            SearchAllocationContextGateway contexts,
            SearchExperimentTransactionGateway transactions,
            ObjectMapper mapper) {
        return ExperimentExecutionModuleFactory.allocation(
                runs, search.generation(), contexts, transactions, Clock.systemUTC(), mapper);
    }

    @Bean
    @ConditionalOnBean({SearchCandidateAllocationUseCase.class, TrustedSearchCoordinationUseCase.class})
    SearchCoordinator searchCoordinator(
            SearchCandidateAllocationUseCase allocations,
            WorkerProperties properties,
            SearchRunStore runs,
            TrustedSearchCoordinationUseCase trusted,
            SearchModuleFactory.Components search) {
        return new SearchCoordinator(allocations, properties, runs, trusted, search, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnBean(SearchCoordinator.class)
    SearchRequestConsumer searchRequestConsumer(
            RedisStreamMessageReader reader,
            SearchCoordinator coordinator,
            WorkerProperties properties,
            ObjectMapper mapper) {
        return new SearchRequestConsumer(reader, coordinator, properties, mapper);
    }

    @Bean
    @ConditionalOnBean(SearchCoordinator.class)
    SearchCompletionConsumer searchCompletionConsumer(
            RedisStreamMessageReader reader,
            SearchCoordinator coordinator,
            WorkerProperties properties,
            ObjectMapper mapper) {
        return new SearchCompletionConsumer(reader, coordinator, properties, mapper);
    }

    @Bean
    @ConditionalOnBean(TrustedSearchCoordinationUseCase.class)
    SearchReconciler searchReconciler(
            SearchRunStore runs,
            TrustedSearchCoordinationUseCase coordination,
            SearchReproductionVerificationUseCase reproductions,
            WorkerProperties properties) {
        return new SearchReconciler(runs, coordination, Clock.systemUTC(),
                properties.reconciliation().staleGracePeriod(),
                properties.reconciliation().searchBatchSize(), reproductions);
    }

    @Bean
    SearchObservability searchObservability(MeterRegistry meters) {
        return new SearchObservability(meters);
    }

    @Bean
    SearchFailureHandler searchFailureHandler(
            WorkerProperties properties,
            DeadLetterPublisher deadLetters,
            LifecycleNotificationPublisher lifecycle,
            SearchObservability observability) {
        return new SearchFailureHandler(properties, deadLetters, lifecycle, observability);
    }
}

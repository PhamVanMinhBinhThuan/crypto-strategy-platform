package com.cryptostrategy.platform.execution.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.execution.api.port.in.SearchCoordinationCommand;
import com.cryptostrategy.platform.execution.api.port.out.SearchAllocationContextGateway;
import com.cryptostrategy.platform.execution.api.port.out.SearchAllocationResult;
import com.cryptostrategy.platform.execution.api.port.out.SearchExperimentTransactionGateway;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.search.api.SearchModuleFactory;
import com.cryptostrategy.platform.search.api.model.CompositeSearchSpace;
import com.cryptostrategy.platform.search.api.model.SearchCombinationPolicy;
import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunMode;
import com.cryptostrategy.platform.search.api.model.SearchStopConditions;
import com.cryptostrategy.platform.search.api.model.SearchStrategyPoolEntry;
import com.cryptostrategy.platform.search.api.model.SearchSpace;
import com.cryptostrategy.platform.search.api.model.SearchExperimentId;
import com.cryptostrategy.platform.search.api.model.SearchJobId;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import com.cryptostrategy.platform.search.api.port.out.SearchRunClaim;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SearchCandidateAllocationServiceTest {
    private static final String RUN_ID = "01J00000000000000000000901";
    private static final String EXPERIMENT_ID = "01J00000000000000000000902";
    private static final String SEARCH_JOB_ID = "01J00000000000000000000903";
    private static final Instant NOW = Instant.parse("2026-09-05T04:00:00Z");

    @Test
    void topKDoesNotCapTheWindowAndV2DoesNotNeedPriorFingerprintMaterialization() {
        SearchRunStore runs = mock(SearchRunStore.class);
        var definition = SearchModuleFactory.baselineDefinition(42L);
        SearchRun initial = SearchRun.pending(
                new SearchRunId(RUN_ID), new SearchExperimentId(EXPERIMENT_ID),
                new SearchJobId(SEARCH_JOB_ID), SearchRunMode.GENERATION, null,
                definition.descriptor(), 42L, "sha256:" + "9".repeat(64),
                definition.initialState(), new SearchStopConditions(100, Duration.ofHours(1)),
                4, NOW.minusSeconds(1)).start(NOW);
        AtomicReference<SearchRun> durable = new AtomicReference<>(initial);
        when(runs.findBySearchJobId(new SearchJobId(SEARCH_JOB_ID)))
                .thenAnswer(ignored -> Optional.of(durable.get()));
        when(runs.claim(new SearchRunId(RUN_ID)))
                .thenAnswer(ignored -> Optional.of(new SearchRunClaim(
                        durable.get(), durable.get().version())));

        CompositeSearchSpace space = new CompositeSearchSpace(
                List.of(new SearchStrategyPoolEntry(
                        new StrategyReference(
                                new StrategyVersionId("01J00000000000000000000904"),
                                new StrategyPluginId("scale-fixture"),
                                SemanticVersion.parse("1.0.0")),
                        Map.of("period", new SearchParameterDomain(ParameterType.INTEGER,
                                java.util.stream.LongStream.rangeClosed(1, 100)
                                        .mapToObj(StrategyParameterValue.IntegerValue::new)
                                        .map(StrategyParameterValue.class::cast).toList())))),
                1, 1, SearchCombinationPolicy.majorityVote(), List.of());
        SearchAllocationContextGateway contexts = (experimentId, searchJobId) -> Optional.of(
                new SearchAllocationContextGateway.Context(
                        UUID.fromString("00000000-0000-4000-8000-000000000901"),
                        new SearchSpace(Map.of()), Optional.of(space), Set.of(),
                        10, 10, 0));
        SearchExperimentTransactionGateway transactions = mock(SearchExperimentTransactionGateway.class);
        when(transactions.allocate(any())).thenAnswer(invocation -> {
            var allocation = invocation.getArgument(0,
                    com.cryptostrategy.platform.execution.api.port.out.AllocateSearchCandidateCommand.class);
            durable.set(allocation.replacementRun());
            return SearchAllocationResult.allocated(allocation.candidate().candidateId(),
                    allocation.backtestJob().jobId(), allocation.replacementRun().version());
        });
        var service = new SearchCandidateAllocationService(
                runs, SearchModuleFactory.baseline(runs).generation(), contexts, transactions,
                Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC), new ObjectMapper());

        var result = service.fillAvailableSlots(new SearchCoordinationCommand(
                new JobId(SEARCH_JOB_ID), new ExperimentId(EXPERIMENT_ID),
                4, 20, 1, "f015-window"));

        assertThat(result.allocatedWork()).isEqualTo(14);
        assertThat(result.activeWork()).isEqualTo(4);
        verify(transactions, times(4)).allocate(any());
    }
}

package com.cryptostrategy.platform.worker.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.contracts.api.SearchRequestPayload;
import com.cryptostrategy.platform.execution.api.port.in.SearchCandidateAllocationUseCase;
import com.cryptostrategy.platform.execution.api.port.in.SearchCoordinationCommand;
import com.cryptostrategy.platform.execution.api.port.in.SearchCoordinationResult;
import com.cryptostrategy.platform.search.api.model.SearchRunStatus;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import com.cryptostrategy.platform.search.api.model.SearchExperimentId;
import com.cryptostrategy.platform.search.api.model.SearchJobId;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import com.cryptostrategy.platform.search.api.SearchModuleFactory;
import com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.search.coordination.SearchCoordinator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SearchCoordinatorTest {
    @Test
    void clampsRequestedWindowToPerExperimentLimitAndReturnsAuthoritativeProgress() {
        SearchCandidateAllocationUseCase allocations = mock(SearchCandidateAllocationUseCase.class);
        WorkerProperties properties = new WorkerProperties(
                null, null, null,
                new WorkerProperties.Concurrency(4, 2, 2, 20, 3),
                null, null, null, null);
        SearchRunStore runs = mock(SearchRunStore.class);
        SearchRun durable = mock(SearchRun.class);
        when(durable.experimentId()).thenReturn(new SearchExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W3"));
        when(durable.searchRunId()).thenReturn(new SearchRunId("01J7K8M9N0P1Q2R3S4T5A6V7W4"));
        when(durable.searchJobId()).thenReturn(new SearchJobId("01J7K8M9N0P1Q2R3S4T5A6V7W2"));
        when(durable.status()).thenReturn(SearchRunStatus.RUNNING);
        when(runs.findBySearchJobId(new SearchJobId("01J7K8M9N0P1Q2R3S4T5A6V7W2")))
                .thenReturn(Optional.of(durable));
        TrustedSearchCoordinationUseCase trusted = mock(TrustedSearchCoordinationUseCase.class);
        SearchRunId runId = new SearchRunId("01J7K8M9N0P1Q2R3S4T5A6V7W4");
        when(trusted.reconcileRun(any())).thenReturn(new TrustedSearchCoordinationUseCase.CoordinationOutcome(
                runId, 7, 4, 1, SearchRunStatus.RUNNING,
                TrustedSearchCoordinationUseCase.Decision.FILL_AVAILABLE_SLOTS));
        SearchModuleFactory.Components search = mock(SearchModuleFactory.Components.class);
        Instant now = Instant.parse("2026-09-03T06:00:00Z");
        SearchCoordinator coordinator = new SearchCoordinator(allocations, properties, runs, trusted, search,
                Clock.fixed(now, ZoneOffset.UTC));
        SearchCoordinationResult authoritative = new SearchCoordinationResult(
                new com.cryptostrategy.platform.search.api.model.SearchRunId("01J7K8M9N0P1Q2R3S4T5A6V7W4"), 7, 2, 4, 1, SearchRunStatus.RUNNING);
        when(allocations.fillAvailableSlots(any())).thenReturn(authoritative);

        SearchCoordinationResult result = coordinator.coordinate(
                new SearchRequestPayload(
                        new com.cryptostrategy.platform.contracts.api.MessageUlid("01J7K8M9N0P1Q2R3S4T5A6V7W2"),
                        new com.cryptostrategy.platform.contracts.api.MessageUlid("01J7K8M9N0P1Q2R3S4T5A6V7W3"),
                        10,
                        5),
                "correlation-f010");

        assertThat(result).isSameAs(authoritative);
        verify(allocations).fillAvailableSlots(new SearchCoordinationCommand(
                new com.cryptostrategy.platform.experiment.api.job.JobId("01J7K8M9N0P1Q2R3S4T5A6V7W2"),
                new com.cryptostrategy.platform.experiment.api.ExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W3"),
                3,
                20,
                5,
                "correlation-f010"));
        verify(trusted).reconcileRun(new TrustedSearchCoordinationUseCase.ReconciliationTrigger(
                new com.cryptostrategy.platform.experiment.api.ExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W3"),
                now, "correlation-f010"));
        assertThat(result.activeWork()).isEqualTo(2);
        assertThat(result.completedWork()).isEqualTo(4);
        assertThat(result.failedWork()).isEqualTo(1);
    }

    @Test
    void completionDecisionImmediatelyRefillsUsingRunWindowNotTopK() {
        SearchCandidateAllocationUseCase allocations = mock(SearchCandidateAllocationUseCase.class);
        WorkerProperties properties = new WorkerProperties(
                null, null, null,
                new WorkerProperties.Concurrency(4, 2, 2, 20, 3),
                null, null, null, null);
        SearchRunStore runs = mock(SearchRunStore.class);
        SearchRun durable = mock(SearchRun.class);
        var experimentId = new SearchExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W3");
        var searchJobId = new SearchJobId("01J7K8M9N0P1Q2R3S4T5A6V7W2");
        when(durable.experimentId()).thenReturn(experimentId);
        when(durable.searchJobId()).thenReturn(searchJobId);
        when(durable.maxInFlight()).thenReturn(10);
        when(durable.mode()).thenReturn(com.cryptostrategy.platform.search.api.model.SearchRunMode.GENERATION);
        when(runs.findByExperimentId(experimentId)).thenReturn(Optional.of(durable));
        TrustedSearchCoordinationUseCase trusted = mock(TrustedSearchCoordinationUseCase.class);
        when(trusted.reconcileCompletion(any())).thenReturn(
                new TrustedSearchCoordinationUseCase.CoordinationOutcome(
                        new SearchRunId("01J7K8M9N0P1Q2R3S4T5A6V7W4"),
                        8, 5, 0, SearchRunStatus.RUNNING,
                        TrustedSearchCoordinationUseCase.Decision.FILL_AVAILABLE_SLOTS));
        SearchModuleFactory.Components search = mock(SearchModuleFactory.Components.class);
        SearchCoordinator coordinator = new SearchCoordinator(
                allocations, properties, runs, trusted, search, Clock.systemUTC());
        var trigger = new TrustedSearchCoordinationUseCase.CompletionTrigger(
                "01J7K8M9N0P1Q2R3S4T5A6V7X1",
                new com.cryptostrategy.platform.experiment.api.ExperimentId(experimentId.value()),
                new com.cryptostrategy.platform.experiment.api.CandidateId("01J7K8M9N0P1Q2R3S4T5A6V7W5"),
                new com.cryptostrategy.platform.experiment.api.job.JobId("01J7K8M9N0P1Q2R3S4T5A6V7W6"),
                Instant.parse("2026-09-05T00:00:00Z"),
                "correlation-f015");

        coordinator.complete(trigger);

        verify(allocations).fillAvailableSlots(new SearchCoordinationCommand(
                new com.cryptostrategy.platform.experiment.api.job.JobId(searchJobId.value()),
                new com.cryptostrategy.platform.experiment.api.ExperimentId(experimentId.value()),
                3,
                20,
                1,
                "correlation-f015"));
    }
}

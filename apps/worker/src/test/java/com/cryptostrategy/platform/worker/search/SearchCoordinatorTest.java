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
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.search.coordination.SearchCoordinator;
import org.junit.jupiter.api.Test;

class SearchCoordinatorTest {
    @Test
    void clampsRequestedWindowToPerExperimentLimitAndReturnsAuthoritativeProgress() {
        SearchCandidateAllocationUseCase allocations = mock(SearchCandidateAllocationUseCase.class);
        WorkerProperties properties = new WorkerProperties(
                null, null, null,
                new WorkerProperties.Concurrency(4, 2, 2, 20, 3),
                null, null, null, null);
        SearchCoordinator coordinator = new SearchCoordinator(allocations, properties);
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
                5,
                "correlation-f010"));
        assertThat(result.activeWork()).isEqualTo(2);
        assertThat(result.completedWork()).isEqualTo(4);
        assertThat(result.failedWork()).isEqualTo(1);
    }
}

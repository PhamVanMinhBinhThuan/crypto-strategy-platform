package com.cryptostrategy.platform.worker.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase;
import com.cryptostrategy.platform.search.api.model.GeneratorDescriptor;
import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import com.cryptostrategy.platform.search.api.model.SearchRunMode;
import com.cryptostrategy.platform.search.api.model.SearchStopConditions;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.worker.search.reconciliation.SearchReconciler;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SearchReconciliationTest {
    @Test
    void rebuildsMissingCoordinationIntentFromBoundedDurableScan() {
        SearchRunStore runs = mock(SearchRunStore.class);
        TrustedSearchCoordinationUseCase coordination = mock(TrustedSearchCoordinationUseCase.class);
        Instant now = Instant.parse("2026-09-03T00:10:00Z");
        SearchRun run = SearchRun.pending(new SearchRunId("01J7K8M9N0P1Q2R3S4T5A6V7W1"),
                "01J7K8M9N0P1Q2R3S4T5A6V7W2", "01J7K8M9N0P1Q2R3S4T5A6V7W3",
                SearchRunMode.GENERATION, null,
                new GeneratorDescriptor(new GeneratorId("random-search"), GeneratorVersion.parse("1.0.0"),
                        "random-state-v1", Set.of(ParameterType.INTEGER), "descriptor"),
                7L, "space", new GeneratorState("random-state-v1", "{}", "state"),
                new SearchStopConditions(2, Duration.ofMinutes(5)), 1, now.minusSeconds(600));
        when(runs.findRecoverable(now.minusSeconds(30), 25)).thenReturn(List.of(run));
        var reconciler = new SearchReconciler(runs, coordination,
                Clock.fixed(now, ZoneOffset.UTC), Duration.ofSeconds(30), 25);

        reconciler.reconcile();

        verify(coordination).reconcileRun(any(TrustedSearchCoordinationUseCase.ReconciliationTrigger.class));
    }
}

package com.cryptostrategy.platform.execution.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.execution.api.port.in.StartSearchReproductionUseCase;
import com.cryptostrategy.platform.execution.api.port.out.SearchReproductionGateway;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SearchReproductionValidationTest {
    private static final UUID OWNER = UUID.fromString("93000000-0000-4000-8000-000000000001");
    private static final ExperimentId SOURCE = new ExperimentId("63000000000000000000000001");

    @Test
    void missingOrForeignSourceUsesTheSameInaccessibleOutcome() {
        var gateway = mock(SearchReproductionGateway.class);
        when(gateway.loadSource(OWNER, SOURCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new SearchReproductionApplicationService(gateway).start(command()))
                .isInstanceOf(ResourceInaccessibleException.class)
                .hasMessage("Experiment resource is inaccessible");
    }

    @Test
    void nonTerminalSourceIsRejectedBeforeAtomicCreate() {
        var gateway = mock(SearchReproductionGateway.class);
        when(gateway.loadSource(OWNER, SOURCE)).thenReturn(Optional.of(
                new SearchReproductionGateway.SourceSnapshot(SOURCE, "RUNNING", true, List.of())));

        assertThatThrownBy(() -> new SearchReproductionApplicationService(gateway).start(command()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("terminal");
    }

    @Test
    void sourceWithoutCompleteEvidenceIsRejectedBeforeAtomicCreate() {
        var gateway = mock(SearchReproductionGateway.class);
        when(gateway.loadSource(OWNER, SOURCE)).thenReturn(Optional.of(
                new SearchReproductionGateway.SourceSnapshot(SOURCE, "COMPLETED", false, List.of())));

        assertThatThrownBy(() -> new SearchReproductionApplicationService(gateway).start(command()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("evidence");
    }

    private static StartSearchReproductionUseCase.Command command() {
        Instant now = Instant.parse("2026-09-03T02:00:00Z");
        return new StartSearchReproductionUseCase.Command(OWNER, SOURCE, "reproduce", "key", "hash",
                "correlation", now, now.plusSeconds(3600));
    }
}

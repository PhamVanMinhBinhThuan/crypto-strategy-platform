package com.cryptostrategy.platform.execution.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.execution.api.port.in.StartSearchReproductionUseCase;
import com.cryptostrategy.platform.execution.api.port.out.SearchReproductionGateway;
import com.cryptostrategy.platform.execution.api.port.out.SentimentSnapshotPreflight;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.provenance.SentimentProvenanceSnapshot;
import com.cryptostrategy.platform.strategy.api.model.StrategyInputSnapshotId;
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

    @Test
    void verifiesTheOriginalFrozenSentimentSnapshotBeforeCreatingReproduction() {
        var gateway = mock(SearchReproductionGateway.class);
        var snapshots = mock(SentimentSnapshotPreflight.class);
        var provenance = sentimentProvenance();
        when(gateway.loadSource(OWNER, SOURCE)).thenReturn(Optional.of(
                new SearchReproductionGateway.SourceSnapshot(
                        SOURCE, "COMPLETED", true, List.of(), Optional.of(provenance))));
        when(gateway.create(any())).thenAnswer(invocation -> {
            var create = invocation.getArgument(0, SearchReproductionGateway.CreateCommand.class);
            return new SearchReproductionGateway.Result(
                    SearchReproductionGateway.Result.Status.CREATED,
                    create.experimentId(), create.searchJobId());
        });

        new SearchReproductionApplicationService(gateway, snapshots).start(command());

        verify(snapshots).verify(provenance);
        verify(gateway).create(any());
    }

    @Test
    void unresolvedSentimentSnapshotPreventsPartialReproductionCreation() {
        var gateway = mock(SearchReproductionGateway.class);
        var snapshots = mock(SentimentSnapshotPreflight.class);
        var provenance = sentimentProvenance();
        when(gateway.loadSource(OWNER, SOURCE)).thenReturn(Optional.of(
                new SearchReproductionGateway.SourceSnapshot(
                        SOURCE, "COMPLETED", true, List.of(), Optional.of(provenance))));
        doThrow(new IllegalStateException("Sentiment snapshot is unavailable"))
                .when(snapshots).verify(provenance);

        assertThatThrownBy(() ->
                new SearchReproductionApplicationService(gateway, snapshots).start(command()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Sentiment snapshot");

        verify(gateway, never()).create(any());
    }

    private static SentimentProvenanceSnapshot sentimentProvenance() {
        return new SentimentProvenanceSnapshot(
                new StrategyInputSnapshotId("01J00000000000000000000407"), "sentiment-snapshot-v1",
                "sha256:" + "b".repeat(64), new AssetId("01J00000000000000000000406"),
                Instant.parse("2026-09-03T01:00:00Z"), "multichannel-english",
                "1.0.0", "whitespace-en-v1", "sentiment-v1", 4);
    }

    private static StartSearchReproductionUseCase.Command command() {
        Instant now = Instant.parse("2026-09-03T02:00:00Z");
        return new StartSearchReproductionUseCase.Command(OWNER, SOURCE, "reproduce", "key", "hash",
                "correlation", now, now.plusSeconds(3600));
    }
}

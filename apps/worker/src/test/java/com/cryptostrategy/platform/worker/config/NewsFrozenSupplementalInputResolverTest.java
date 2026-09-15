package com.cryptostrategy.platform.worker.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.provenance.SentimentProvenanceSnapshot;
import com.cryptostrategy.platform.news.api.model.ContentHash;
import com.cryptostrategy.platform.news.api.model.NewsId;
import com.cryptostrategy.platform.news.api.model.SentimentModelRelease;
import com.cryptostrategy.platform.news.api.model.SentimentResultId;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshot;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotId;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotObservation;
import com.cryptostrategy.platform.news.api.port.in.GetSentimentSnapshotUseCase;
import com.cryptostrategy.platform.strategy.api.model.StrategyInputSnapshotId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NewsFrozenSupplementalInputResolverTest {
    private static final AssetId ASSET_ID = new AssetId("01J00000000000000000000406");
    private static final SentimentSnapshotId SNAPSHOT_ID =
            new SentimentSnapshotId("01J00000000000000000000407");
    private static final Instant CUTOFF = Instant.parse("2026-09-03T01:00:00Z");
    private static final SentimentModelRelease RELEASE = new SentimentModelRelease(
            "multichannel-english-1.0.0", "multichannel-english",
            "multichannel-whitespace-en-1", "sentiment-v1");

    @Test
    void frozenReplayUsesThePersistedSnapshotWithoutALiveSentimentDependency() {
        GetSentimentSnapshotUseCase snapshots = Mockito.mock(GetSentimentSnapshotUseCase.class);
        SentimentSnapshot snapshot = snapshot();
        when(snapshots.get(SNAPSHOT_ID)).thenReturn(snapshot);
        ExperimentManifest manifest = Mockito.mock(ExperimentManifest.class);
        when(manifest.sentimentProvenance()).thenReturn(Optional.of(provenance(snapshot)));

        var resolved = new NewsFrozenSupplementalInputResolver(snapshots).resolve(manifest);
        var input = resolved.get("sentiment-polarity");

        assertThat(input.snapshotId().value()).isEqualTo(SNAPSHOT_ID.value());
        assertThat(input.metadata())
                .containsEntry("modelVersion", RELEASE.modelVersion())
                .containsEntry("preprocessingVersion", RELEASE.preprocessingVersion());
        assertThat(input.observations()).singleElement().satisfies(observation -> {
            assertThat(observation.identity().value()).isEqualTo("01J00000000000000000000408");
            assertThat(observation.sourceReference()).isEqualTo("01J00000000000000000000409");
            assertThat(observation.value()).isEqualByComparingTo("0.75");
            assertThat(observation.weight()).isEqualByComparingTo("0.80");
        });
    }

    @Test
    void technicalExecutionIgnoresSentimentAvailabilityAndDoesNotReadItsStore() {
        GetSentimentSnapshotUseCase snapshots = Mockito.mock(GetSentimentSnapshotUseCase.class);
        ExperimentManifest manifest = Mockito.mock(ExperimentManifest.class);
        when(manifest.sentimentProvenance()).thenReturn(Optional.empty());

        assertThat(new NewsFrozenSupplementalInputResolver(snapshots).resolve(manifest)).isEmpty();
        verify(snapshots, never()).get(Mockito.any());
    }

    @Test
    void rejectsSnapshotEvidenceThatNoLongerMatchesTheManifest() {
        GetSentimentSnapshotUseCase snapshots = Mockito.mock(GetSentimentSnapshotUseCase.class);
        SentimentSnapshot snapshot = snapshot();
        when(snapshots.get(SNAPSHOT_ID)).thenReturn(snapshot);
        ExperimentManifest manifest = Mockito.mock(ExperimentManifest.class);
        var mismatched = new SentimentProvenanceSnapshot(
                new StrategyInputSnapshotId(SNAPSHOT_ID.value()), snapshot.schemaVersion(),
                "sha256:" + "d".repeat(64), ASSET_ID, CUTOFF, RELEASE.modelName(),
                RELEASE.modelVersion(), RELEASE.preprocessingVersion(), RELEASE.contractVersion(), 1);
        when(manifest.sentimentProvenance()).thenReturn(Optional.of(mismatched));

        assertThatThrownBy(() -> new NewsFrozenSupplementalInputResolver(snapshots).resolve(manifest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("provenance");
    }

    private static SentimentSnapshot snapshot() {
        var observation = new SentimentSnapshotObservation(
                new NewsId("01J00000000000000000000408"),
                new SentimentResultId("01J00000000000000000000409"), ASSET_ID,
                CUTOFF.minusSeconds(60), CUTOFF.minusSeconds(30),
                new ContentHash("sha256:" + "c".repeat(64)),
                new BigDecimal("0.75"), new BigDecimal("0.80"));
        return new SentimentSnapshot(SNAPSHOT_ID, SentimentSnapshot.SCHEMA_VERSION, ASSET_ID,
                RELEASE, CUTOFF, List.of(observation), "sha256:" + "a".repeat(64),
                CUTOFF.plusSeconds(1));
    }

    private static SentimentProvenanceSnapshot provenance(SentimentSnapshot snapshot) {
        return new SentimentProvenanceSnapshot(
                new StrategyInputSnapshotId(snapshot.snapshotId().value()), snapshot.schemaVersion(),
                snapshot.fingerprint(), snapshot.assetId(), snapshot.publicationCutoff(),
                snapshot.release().modelName(), snapshot.release().modelVersion(),
                snapshot.release().preprocessingVersion(), snapshot.release().contractVersion(),
                snapshot.observationCount());
    }
}

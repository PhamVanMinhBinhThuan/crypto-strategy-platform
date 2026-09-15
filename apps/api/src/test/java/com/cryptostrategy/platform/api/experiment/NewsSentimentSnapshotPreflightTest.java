package com.cryptostrategy.platform.api.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.execution.api.port.out.SentimentSnapshotPreflight;
import com.cryptostrategy.platform.news.api.model.ContentHash;
import com.cryptostrategy.platform.news.api.model.NewsId;
import com.cryptostrategy.platform.news.api.model.SentimentModelRelease;
import com.cryptostrategy.platform.news.api.model.SentimentResultId;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshot;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotId;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotObservation;
import com.cryptostrategy.platform.news.api.port.in.CreateSentimentSnapshotUseCase;
import com.cryptostrategy.platform.news.api.port.in.GetSentimentSnapshotUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NewsSentimentSnapshotPreflightTest {
    private static final AssetId ASSET_ID = new AssetId("01J00000000000000000000406");
    private static final SentimentSnapshotId SNAPSHOT_ID =
            new SentimentSnapshotId("01J00000000000000000000407");
    private static final Instant CUTOFF = Instant.parse("2026-09-03T01:00:00Z");
    private static final SentimentModelRelease RELEASE = new SentimentModelRelease(
            "multichannel-english-1.0.0", "multichannel-english",
            "multichannel-whitespace-en-1", "sentiment-v1");

    @Test
    void freezesUsingTheConfiguredReleaseAndMapsCompleteProvenance() {
        CreateSentimentSnapshotUseCase create = Mockito.mock(CreateSentimentSnapshotUseCase.class);
        GetSentimentSnapshotUseCase get = Mockito.mock(GetSentimentSnapshotUseCase.class);
        SentimentSnapshot snapshot = snapshot("a");
        when(create.create(any())).thenReturn(snapshot);

        var adapter = new NewsSentimentSnapshotPreflight(create, get, RELEASE);
        var result = adapter.freeze(new SentimentSnapshotPreflight.FreezeRequest(
                UUID.fromString("93000000-0000-4000-8000-000000000001"), ASSET_ID, CUTOFF));

        assertThat(result.snapshotId().value()).isEqualTo(SNAPSHOT_ID.value());
        assertThat(result.assetId()).isEqualTo(ASSET_ID);
        assertThat(result.snapshotFingerprint()).isEqualTo(snapshot.fingerprint());
        assertThat(result.modelName()).isEqualTo(RELEASE.modelName());
        assertThat(result.modelVersion()).isEqualTo(RELEASE.modelVersion());
        assertThat(result.articleCount()).isOne();
        verify(create).create(new CreateSentimentSnapshotUseCase.Command(ASSET_ID, CUTOFF, RELEASE));
    }

    @Test
    void verifiesEveryFrozenFieldInsteadOfSilentlyAcceptingChangedEvidence() {
        CreateSentimentSnapshotUseCase create = Mockito.mock(CreateSentimentSnapshotUseCase.class);
        GetSentimentSnapshotUseCase get = Mockito.mock(GetSentimentSnapshotUseCase.class);
        when(get.get(SNAPSHOT_ID)).thenReturn(snapshot("b"));
        var adapter = new NewsSentimentSnapshotPreflight(create, get, RELEASE);
        var provenance = NewsSentimentSnapshotPreflight.toProvenance(snapshot("a"));

        assertThatThrownBy(() -> adapter.verify(provenance))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("provenance");
    }

    private static SentimentSnapshot snapshot(String fingerprintCharacter) {
        var observation = new SentimentSnapshotObservation(
                new NewsId("01J00000000000000000000408"),
                new SentimentResultId("01J00000000000000000000409"), ASSET_ID,
                CUTOFF.minusSeconds(60), CUTOFF.minusSeconds(30),
                new ContentHash("sha256:" + "c".repeat(64)),
                new BigDecimal("0.75"), new BigDecimal("0.80"));
        return new SentimentSnapshot(SNAPSHOT_ID, SentimentSnapshot.SCHEMA_VERSION, ASSET_ID,
                RELEASE, CUTOFF, List.of(observation),
                "sha256:" + fingerprintCharacter.repeat(64), CUTOFF.plusSeconds(1));
    }
}

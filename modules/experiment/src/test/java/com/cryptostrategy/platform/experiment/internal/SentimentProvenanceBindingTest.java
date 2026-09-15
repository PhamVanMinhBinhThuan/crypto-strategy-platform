package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.SentimentProvenanceSnapshot;
import com.cryptostrategy.platform.strategy.api.model.StrategyInputSnapshotId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SentimentProvenanceBindingTest {

    private static final Instant CUTOFF = Instant.parse("2026-09-09T12:00:00Z");
    private static final String SNAPSHOT_FINGERPRINT = "sha256:" + "a".repeat(64);

    @Test
    @DisplayName("Sentiment provenance has a versioned, canonical configuration round trip")
    void versionedCanonicalRoundTrip() {
        SentimentProvenanceSnapshot provenance = provenance();

        Map<String, Object> config = provenance.toConfig();

        assertThat(config)
                .containsEntry("provenanceVersion", SentimentProvenanceSnapshot.PROVENANCE_VERSION)
                .containsEntry("snapshotId", "01K4A000000000000000000001")
                .containsEntry("schemaVersion", "sentiment-snapshot-v1")
                .containsEntry("snapshotFingerprint", SNAPSHOT_FINGERPRINT)
                .containsEntry("publicationCutoff", CUTOFF.toString())
                .containsEntry("articleCount", 4);
        assertThat(SentimentProvenanceSnapshot.fromConfig(config)).contains(provenance);
        assertThatThrownBy(() -> config.put("articleCount", 5))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Legacy sentiment configuration remains readable while versioned values are strict")
    void preservesLegacyCompatibilityAndRejectsInvalidVersionedValues() {
        assertThat(SentimentProvenanceSnapshot.fromConfig(null)).isEmpty();
        assertThat(SentimentProvenanceSnapshot.fromConfig(Map.of("enabled", true))).isEmpty();

        Map<String, Object> unsupported = new java.util.HashMap<>(provenance().toConfig());
        unsupported.put("provenanceVersion", "sentiment-provenance-v2");
        assertThatThrownBy(() -> SentimentProvenanceSnapshot.fromConfig(unsupported))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported Sentiment provenance version");

        assertThatThrownBy(() -> new SentimentProvenanceSnapshot(
                new StrategyInputSnapshotId("01K4A000000000000000000001"),
                "sentiment-snapshot-v1",
                "sha256:invalid",
                new AssetId("01K4A000000000000000000002"),
                CUTOFF,
                "multichannel-english",
                "1.0.0",
                "whitespace-en-v1",
                "sentiment-v1",
                4
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("snapshotFingerprint");
    }

    @Test
    @DisplayName("Manifest exposes typed provenance without changing the legacy sentimentConfig contract")
    void manifestTypedBindingIsBackwardCompatible() {
        ExperimentManifest legacy = manifest(Map.of("enabled", true));
        SentimentProvenanceSnapshot provenance = provenance();

        ExperimentManifest versioned = legacy
                .withFingerprint("sha256:" + "d".repeat(64))
                .withSentimentProvenance(provenance);

        assertThat(legacy.sentimentProvenance()).isEmpty();
        assertThat(versioned.sentimentConfig()).isEqualTo(provenance.toConfig());
        assertThat(versioned.sentimentProvenance()).contains(provenance);
        assertThat(versioned.fingerprint()).isNull();
        assertThat(versioned.withFingerprint("sha256:" + "b".repeat(64)).sentimentProvenance())
                .contains(provenance);
    }

    private SentimentProvenanceSnapshot provenance() {
        return new SentimentProvenanceSnapshot(
                new StrategyInputSnapshotId("01K4A000000000000000000001"),
                "sentiment-snapshot-v1",
                SNAPSHOT_FINGERPRINT,
                new AssetId("01K4A000000000000000000002"),
                CUTOFF,
                "multichannel-english",
                "1.0.0",
                "whitespace-en-v1",
                "sentiment-v1",
                4
        );
    }

    private ExperimentManifest manifest(Map<String, Object> sentimentConfig) {
        DatasetProvenanceSnapshot dataset = new DatasetProvenanceSnapshot(
                new DatasetVersionId("01ARZ3NDEKTSV4RRFFQ69G5FAV"),
                "candle-v1",
                "sha256:" + "c".repeat(64),
                "BINANCE",
                "BTC/USDT",
                "1h",
                "normalization-v1",
                Instant.parse("2026-09-01T00:00:00Z"),
                CUTOFF,
                200
        );
        return new ExperimentManifest(
                new ExperimentId("01ARZ3NDEKTSV4RRFFQ69G5FAW"),
                "manifest-v1",
                dataset,
                ProvenanceTestFixtures.single("sentiment-polarity", "1.0.0", Map.of("lookbackHours", 24), null),
                Map.of(),
                Map.of(),
                Map.of(),
                sentimentConfig,
                "0.1.0",
                "abcdef123456",
                null,
                CUTOFF
        );
    }
}

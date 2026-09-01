package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalFingerprintCalculatorTest {

    private final CanonicalFingerprintCalculator calculator = new CanonicalFingerprintCalculator();

    private ExperimentManifest createSampleManifest() {
        DatasetProvenanceSnapshot dataset = new DatasetProvenanceSnapshot(
                new DatasetVersionId("01ARZ3NDEKTSV4RRFFQ69G5FAV"),
                "candle-v1",
                "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "BINANCE",
                "BTC/USDT",
                "1m",
                "v1",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                1440
        );

        StrategyProvenanceSnapshot strategy = ProvenanceTestFixtures.single(
                "sma-crossover",
                "1.0.0",
                Map.of("fastPeriod", 10, "slowPeriod", 20),
                null
        );

        return new ExperimentManifest(
                new ExperimentId("01ARZ3NDEKTSV4RRFFQ69G5FAV"),
                "manifest-v1",
                dataset,
                strategy,
                Map.of("initialCapital", "10000.00", "feeRate", "0.001"),
                Map.of("searchMethod", "GRID"),
                Map.of("targetMetric", "SHARPE"),
                null,
                "0.1.0",
                "abcdef1234567890",
                null,
                Instant.parse("2026-08-30T12:00:00Z")
        );
    }

    @Test
    @DisplayName("Fingerprint calculation is deterministic and starts with sha256:")
    void deterministicFingerprint() {
        ExperimentManifest manifest1 = createSampleManifest();
        ExperimentManifest manifest2 = createSampleManifest();

        String fp1 = calculator.calculate(manifest1);
        String fp2 = calculator.calculate(manifest2);

        assertThat(fp1).isEqualTo(fp2);
        assertThat(fp1).startsWith("sha256:").hasSize(71); // "sha256:" (7) + 64 hex chars
    }

    @Test
    @DisplayName("Key re-ordering in nested maps produces identical fingerprint")
    void keyReorderingDeterminism() {
        ExperimentManifest manifest1 = createSampleManifest();

        Map<String, Object> map1 = new LinkedHashMap<>();
        map1.put("b", "val2");
        map1.put("a", "val1");

        Map<String, Object> map2 = new LinkedHashMap<>();
        map2.put("a", "val1");
        map2.put("b", "val2");

        DatasetProvenanceSnapshot dataset = manifest1.datasetProvenance();
        StrategyProvenanceSnapshot strategy1 = ProvenanceTestFixtures.single("sma-crossover", "1.0.0", map1, null);
        StrategyProvenanceSnapshot strategy2 = ProvenanceTestFixtures.single("sma-crossover", "1.0.0", map2, null);

        ExperimentManifest m1 = new ExperimentManifest(manifest1.experimentId(), manifest1.manifestVersion(), dataset, strategy1, manifest1.backtestConfig(), manifest1.searchConfig(), manifest1.evaluationConfig(), manifest1.sentimentConfig(), manifest1.softwareVersion(), manifest1.gitCommit(), null, manifest1.createdAt());
        ExperimentManifest m2 = new ExperimentManifest(manifest1.experimentId(), manifest1.manifestVersion(), dataset, strategy2, manifest1.backtestConfig(), manifest1.searchConfig(), manifest1.evaluationConfig(), manifest1.sentimentConfig(), manifest1.softwareVersion(), manifest1.gitCommit(), null, manifest1.createdAt());

        assertThat(calculator.calculate(m1)).isEqualTo(calculator.calculate(m2));
    }

    @Test
    @DisplayName("Modifying any field changes the calculated fingerprint")
    void fieldMutationChangesFingerprint() {
        ExperimentManifest original = createSampleManifest();
        String originalFp = calculator.calculate(original);

        StrategyProvenanceSnapshot modifiedStrategy = ProvenanceTestFixtures.single(
                "sma-crossover",
                "1.0.0",
                Map.of("fastPeriod", 11, "slowPeriod", 20),
                null
        );
        ExperimentManifest modified = new ExperimentManifest(
                original.experimentId(),
                original.manifestVersion(),
                original.datasetProvenance(),
                modifiedStrategy,
                original.backtestConfig(),
                original.searchConfig(),
                original.evaluationConfig(),
                original.sentimentConfig(),
                original.softwareVersion(),
                original.gitCommit(),
                null,
                original.createdAt()
        );

        String modifiedFp = calculator.calculate(modified);
        assertThat(modifiedFp).isNotEqualTo(originalFp);
    }
}

package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.error.ExperimentValidationException;
import com.cryptostrategy.platform.experiment.api.error.InvalidStateTransitionException;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExperimentAggregateTest {

    private final UUID ownerUserId = UUID.randomUUID();
    private final ExperimentId experimentId = ExperimentId.generate();

    private ExperimentManifest createDraftManifest() {
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
                experimentId,
                "manifest-v1",
                dataset,
                strategy,
                Map.of("initialCapital", "10000.00"),
                Map.of("searchMethod", "GRID"),
                Map.of("targetMetric", "SHARPE"),
                null,
                "0.1.0",
                "gitcommit123",
                null,
                Instant.now()
        );
    }

    @Test
    @DisplayName("Experiment starts in CREATED status with mutable draft manifest")
    void experimentCreation() {
        Experiment experiment = Experiment.create(experimentId, ownerUserId, "Test Experiment", null, null, Instant.now());
        ExperimentManifest draftManifest = createDraftManifest();

        ExperimentAggregate aggregate = new ExperimentAggregate(experiment, draftManifest);

        assertThat(aggregate.getExperiment().status()).isEqualTo(ExperimentStatus.CREATED);
        assertThat(aggregate.getManifest().fingerprint()).isNull();

        // Mutable while CREATED
        ExperimentManifest updatedDraft = draftManifest.withFingerprint(null);
        aggregate.updateDraftManifest(updatedDraft);
        assertThat(aggregate.getManifest()).isNotNull();
    }

    @Test
    @DisplayName("Freezing transitions to QUEUED, assigns non-empty fingerprint, and forbids subsequent edits")
    void freezeAndQueue() {
        Experiment experiment = Experiment.create(experimentId, ownerUserId, "Test Experiment", null, null, Instant.now());
        ExperimentManifest draftManifest = createDraftManifest();
        ExperimentAggregate aggregate = new ExperimentAggregate(experiment, draftManifest);

        String fingerprint = "sha256:1111222233334444555566667777888899990000aaaabbbbccccddddeeeeffff";
        Instant queuedAt = Instant.now();

        aggregate.freezeAndQueue(fingerprint, queuedAt);

        assertThat(aggregate.getExperiment().status()).isEqualTo(ExperimentStatus.QUEUED);
        assertThat(aggregate.getManifest().fingerprint()).isEqualTo(fingerprint);

        // Attempting to update after freeze fails
        assertThatThrownBy(() -> aggregate.updateDraftManifest(draftManifest))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("Freezing with empty or null fingerprint is rejected")
    void freezeValidation() {
        Experiment experiment = Experiment.create(experimentId, ownerUserId, "Test Experiment", null, null, Instant.now());
        ExperimentManifest draftManifest = createDraftManifest();
        ExperimentAggregate aggregate = new ExperimentAggregate(experiment, draftManifest);

        assertThatThrownBy(() -> aggregate.freezeAndQueue("", Instant.now()))
                .isInstanceOf(ExperimentValidationException.class);
        assertThatThrownBy(() -> aggregate.freezeAndQueue(null, Instant.now()))
                .isInstanceOf(ExperimentValidationException.class);
    }
}

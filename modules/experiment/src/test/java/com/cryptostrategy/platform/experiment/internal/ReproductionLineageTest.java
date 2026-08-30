package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReproductionLineageTest {

    private static class FakeExperimentStore implements ExperimentStore {
        private final Map<ExperimentId, Experiment> experiments = new HashMap<>();
        private final Map<ExperimentId, ExperimentManifest> manifests = new HashMap<>();

        @Override
        public void insertExperiment(UUID ownerUserId, Experiment experiment, ExperimentManifest draftManifest) {
            experiments.put(experiment.experimentId(), experiment);
            manifests.put(draftManifest.experimentId(), draftManifest);
        }

        @Override
        public Optional<Experiment> findExperimentById(UUID ownerUserId, ExperimentId experimentId) {
            return Optional.ofNullable(experiments.get(experimentId));
        }

        @Override
        public Optional<ExperimentManifest> findManifestByExperimentId(UUID ownerUserId, ExperimentId experimentId) {
            return Optional.ofNullable(manifests.get(experimentId));
        }

        @Override
        public void updateManifest(UUID ownerUserId, ExperimentId experimentId, ExperimentManifest updatedManifest) {}

        @Override
        public void freezeAndQueueExperiment(UUID ownerUserId, ExperimentId experimentId, String fingerprint, Instant queuedAt, OutboxEvent outboxEvent) {}

        @Override
        public void updateExperimentStatus(UUID ownerUserId, ExperimentId experimentId, ExperimentStatus newStatus, Instant updatedAt) {}

        @Override
        public void stopExperimentWithOutbox(UUID ownerUserId, ExperimentId experimentId, OutboxEvent outboxEvent, Instant updatedAt) {}

        @Override
        public void insertCandidate(UUID ownerUserId, CandidateDefinition candidate) {}

        @Override
        public List<CandidateDefinition> listCandidatesByExperimentId(UUID ownerUserId, ExperimentId experimentId) { return List.of(); }

        @Override
        public Optional<CandidateDefinition> findCandidateById(UUID ownerUserId, CandidateId candidateId) { return Optional.empty(); }
    }

    private final FakeExperimentStore store = new FakeExperimentStore();
    private final CanonicalFingerprintCalculator calculator = new CanonicalFingerprintCalculator();
    private final ExperimentApplicationService service = new ExperimentApplicationService(store, calculator);
    private final UUID ownerUserId = UUID.randomUUID();

    @Test
    @DisplayName("Reproduction Run creates a new Experiment entity with reproducesExperimentId pointing to source")
    void reproductionRunCreatesLinkedNewExperiment() {
        ExperimentManifest originalManifest = new ExperimentManifest(
                ExperimentId.generate(),
                "manifest-v1",
                new DatasetProvenanceSnapshot(new DatasetVersionId("01ARZ3NDEKTSV4RRFFQ69G5FAV"), "candle-v1", "sha256:1234", "BINANCE", "BTC/USDT", "1m", "v1", Instant.EPOCH, Instant.EPOCH, 0),
                StrategyProvenanceSnapshot.single("sma", "1.0", Map.of(), null),
                Map.of(), Map.of(), Map.of(), null, "0.1", "gitcommit", "sha256:origfp", Instant.now()
        );

        Experiment original = service.createExperiment(ownerUserId, "Original Experiment", originalManifest, null, null);

        Experiment reproduced = service.reproduceExperiment(ownerUserId, original.experimentId(), "Reproduction Run 1");

        assertThat(reproduced.experimentId()).isNotEqualTo(original.experimentId());
        assertThat(reproduced.reproducesExperimentId()).isEqualTo(original.experimentId());
        assertThat(reproduced.name()).isEqualTo("Reproduction Run 1");
        assertThat(reproduced.status()).isEqualTo(ExperimentStatus.CREATED);

        // Original is unmodified
        assertThat(service.getExperiment(ownerUserId, original.experimentId()).orElseThrow().reproducesExperimentId()).isNull();
    }
}

package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExperimentOwnershipAuthorizationTest {

    private static class InMemoryExperimentStore implements ExperimentStore {
        private final Map<ExperimentId, Experiment> experiments = new HashMap<>();
        private final Map<ExperimentId, ExperimentManifest> manifests = new HashMap<>();

        @Override
        public void insertExperiment(UUID ownerUserId, Experiment experiment, ExperimentManifest draftManifest) {
            experiments.put(experiment.experimentId(), experiment);
            manifests.put(draftManifest.experimentId(), draftManifest);
        }

        @Override
        public Optional<Experiment> findExperimentById(UUID ownerUserId, ExperimentId experimentId) {
            Experiment exp = experiments.get(experimentId);
            if (exp != null && exp.ownerUserId().equals(ownerUserId)) {
                return Optional.of(exp);
            }
            return Optional.empty();
        }

        @Override
        public Optional<ExperimentManifest> findManifestByExperimentId(UUID ownerUserId, ExperimentId experimentId) {
            Experiment exp = experiments.get(experimentId);
            if (exp != null && exp.ownerUserId().equals(ownerUserId)) {
                return Optional.ofNullable(manifests.get(experimentId));
            }
            return Optional.empty();
        }

        @Override
        public void updateManifest(UUID ownerUserId, ExperimentId experimentId, ExperimentManifest updatedManifest) {
            manifests.put(experimentId, updatedManifest);
        }

        @Override
        public void freezeAndQueueExperiment(UUID ownerUserId, ExperimentId experimentId, String fingerprint, Instant queuedAt, OutboxEvent outboxEvent) {
            Experiment exp = experiments.get(experimentId);
            experiments.put(experimentId, new Experiment(exp.experimentId(), exp.ownerUserId(), exp.name(), ExperimentStatus.QUEUED, exp.derivedFromExperimentId(), exp.reproducesExperimentId(), queuedAt, null, null, null, exp.createdAt()));
        }

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

    private final InMemoryExperimentStore store = new InMemoryExperimentStore();
    private final CanonicalFingerprintCalculator calculator = new CanonicalFingerprintCalculator();
    private final ExperimentApplicationService service = new ExperimentApplicationService(store, calculator);

    private final UUID userA = UUID.randomUUID();
    private final UUID userB = UUID.randomUUID();

    @Test
    @DisplayName("User B querying or freezing User A's experiment throws ResourceInaccessibleException without leakage")
    void ownershipIsolation() {
        ExperimentManifest draft = new ExperimentManifest(
                ExperimentId.generate(),
                "v1",
                new DatasetProvenanceSnapshot(new DatasetVersionId("01ARZ3NDEKTSV4RRFFQ69G5FAV"), "candle-v1", "sha256:abcd", "BINANCE", "BTC/USDT", "1m", "v1", Instant.EPOCH, Instant.EPOCH, 0),
                ProvenanceTestFixtures.single("sma", "1.0", Map.of(), null),
                Map.of(), Map.of(), Map.of(), null, "0.1", "git", null, Instant.now()
        );

        Experiment exp = service.createExperiment(userA, "User A Experiment", draft, null, null);

        // User A can access
        assertThat(service.getExperiment(userA, exp.experimentId())).isPresent();

        // User B cannot access
        assertThat(service.getExperiment(userB, exp.experimentId())).isEmpty();

        // User B cannot freeze
        assertThatThrownBy(() -> service.freezeAndQueue(userB, exp.experimentId()))
                .isInstanceOf(ResourceInaccessibleException.class);
    }
}

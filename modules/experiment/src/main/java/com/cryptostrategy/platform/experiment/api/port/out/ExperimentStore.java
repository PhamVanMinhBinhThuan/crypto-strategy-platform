package com.cryptostrategy.platform.experiment.api.port.out;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperimentStore {
    void insertExperiment(UUID ownerUserId, Experiment experiment, ExperimentManifest draftManifest);
    Optional<Experiment> findExperimentById(UUID ownerUserId, ExperimentId experimentId);
    Optional<ExperimentManifest> findManifestByExperimentId(UUID ownerUserId, ExperimentId experimentId);
    void updateManifest(UUID ownerUserId, ExperimentId experimentId, ExperimentManifest updatedManifest);
    void freezeAndQueueExperiment(UUID ownerUserId, ExperimentId experimentId, String fingerprint, Instant queuedAt, OutboxEvent outboxEvent);
    void updateExperimentStatus(UUID ownerUserId, ExperimentId experimentId, ExperimentStatus newStatus, Instant updatedAt);
    void stopExperimentWithOutbox(UUID ownerUserId, ExperimentId experimentId, OutboxEvent outboxEvent, Instant updatedAt);
    void insertCandidate(UUID ownerUserId, CandidateDefinition candidate);
    List<CandidateDefinition> listCandidatesByExperimentId(UUID ownerUserId, ExperimentId experimentId);
    Optional<CandidateDefinition> findCandidateById(UUID ownerUserId, CandidateId candidateId);
}

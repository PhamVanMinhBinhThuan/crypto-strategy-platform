package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentSummary;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GetExperimentUseCase {
    Optional<Experiment> getExperiment(UUID ownerUserId, ExperimentId experimentId);
    Optional<ExperimentManifest> getManifest(UUID ownerUserId, ExperimentId experimentId);
    default List<ExperimentSummary> listRecent(
            UUID ownerUserId, Instant beforeCreatedAt, String beforeExperimentId, int limit) {
        return List.of();
    }
    default long count(UUID ownerUserId) { return 0; }
}

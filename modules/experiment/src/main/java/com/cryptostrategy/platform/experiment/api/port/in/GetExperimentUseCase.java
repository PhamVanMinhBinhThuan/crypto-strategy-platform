package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;

import java.util.Optional;
import java.util.UUID;

public interface GetExperimentUseCase {
    Optional<Experiment> getExperiment(UUID ownerUserId, ExperimentId experimentId);
    Optional<ExperimentManifest> getManifest(UUID ownerUserId, ExperimentId experimentId);
}

package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;

import java.util.UUID;

public interface CreateExperimentUseCase {
    Experiment createExperiment(
            UUID ownerUserId,
            String name,
            ExperimentManifest draftManifest,
            ExperimentId derivedFromExperimentId,
            ExperimentId reproducesExperimentId
    );
}

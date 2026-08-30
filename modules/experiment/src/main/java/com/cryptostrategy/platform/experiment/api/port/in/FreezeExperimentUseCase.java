package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;

import java.util.UUID;

public interface FreezeExperimentUseCase {
    Experiment freezeAndQueue(UUID ownerUserId, ExperimentId experimentId);
}

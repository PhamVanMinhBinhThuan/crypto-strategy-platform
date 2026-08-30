package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.Job;

import java.util.UUID;

public interface CreateSearchJobUseCase {
    Job createSearchJob(UUID ownerUserId, ExperimentId experimentId, String correlationId, int totalGenerations);
}

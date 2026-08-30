package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.Job;

import java.util.UUID;

public interface CreateBacktestJobUseCase {
    Job createBacktestJob(UUID ownerUserId, ExperimentId experimentId, CandidateId candidateId, String correlationId);
}

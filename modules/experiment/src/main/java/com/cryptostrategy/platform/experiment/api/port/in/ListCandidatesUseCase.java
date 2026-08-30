package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.ExperimentId;

import java.util.List;
import java.util.UUID;

public interface ListCandidatesUseCase {
    List<CandidateDefinition> listCandidates(UUID ownerUserId, ExperimentId experimentId);
}

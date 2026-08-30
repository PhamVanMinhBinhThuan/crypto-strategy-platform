package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;

import java.util.Map;
import java.util.UUID;

public interface CreateCandidateUseCase {
    CandidateDefinition createCandidate(
            UUID ownerUserId,
            ExperimentId experimentId,
            int generationIndex,
            Map<String, Object> definition,
            Map<String, Object> generatorState,
            String fingerprint
    );
}

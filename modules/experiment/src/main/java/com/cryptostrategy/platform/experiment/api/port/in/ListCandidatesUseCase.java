package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListCandidatesUseCase {
    List<CandidateDefinition> listCandidates(UUID ownerUserId, ExperimentId experimentId);

    /** Returns at most {@code limit} candidates after the exclusive deterministic key. */
    List<CandidateDefinition> listCandidates(
            UUID ownerUserId,
            ExperimentId experimentId,
            int afterGenerationIndex,
            String afterCandidateId,
            int limit);

    Optional<CandidateDefinition> getCandidate(
            UUID ownerUserId, ExperimentId experimentId, CandidateId candidateId);
}

package com.cryptostrategy.platform.leaderboard.api.port.in;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardSnapshot;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardCandidateEvidence;
import com.cryptostrategy.platform.leaderboard.api.model.CandidatePipelineEntry;
import java.util.List;
import java.util.Optional;

public interface GetLeaderboardUseCase {
    Optional<LeaderboardSnapshot> getLatest(ExperimentId experimentId);
    default Optional<LeaderboardCandidateEvidence> getCandidate(
            ExperimentId experimentId, CandidateId candidateId) {
        return Optional.empty();
    }

    default List<CandidatePipelineEntry> getCandidatePipeline(ExperimentId experimentId) {
        return List.of();
    }

    default Optional<CandidatePipelineEntry> getCandidatePipelineEntry(
            ExperimentId experimentId, CandidateId candidateId) {
        return Optional.empty();
    }
}

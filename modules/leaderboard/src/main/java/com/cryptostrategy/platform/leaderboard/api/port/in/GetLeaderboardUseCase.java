package com.cryptostrategy.platform.leaderboard.api.port.in;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardSnapshot;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardCandidateEvidence;
import java.util.Optional;

public interface GetLeaderboardUseCase {
    Optional<LeaderboardSnapshot> getLatest(ExperimentId experimentId);
    default Optional<LeaderboardCandidateEvidence> getCandidate(
            ExperimentId experimentId, CandidateId candidateId) {
        return Optional.empty();
    }
}

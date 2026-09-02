package com.cryptostrategy.platform.leaderboard.api.port.in;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardSnapshot;
import java.util.Optional;

public interface GetLeaderboardUseCase {
    Optional<LeaderboardSnapshot> getLatest(ExperimentId experimentId);
}

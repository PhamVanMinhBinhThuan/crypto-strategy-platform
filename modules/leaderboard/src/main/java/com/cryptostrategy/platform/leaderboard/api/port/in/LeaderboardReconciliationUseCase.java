package com.cryptostrategy.platform.leaderboard.api.port.in;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevision;

import java.util.Optional;

public interface LeaderboardReconciliationUseCase {
    Optional<LeaderboardRevision> reconcileLeaderboard(ExperimentId experimentId, int batchSize);
}

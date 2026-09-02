package com.cryptostrategy.platform.leaderboard.api;

import com.cryptostrategy.platform.leaderboard.api.port.in.LeaderboardReconciliationUseCase;
import com.cryptostrategy.platform.leaderboard.api.port.in.GetLeaderboardUseCase;
import com.cryptostrategy.platform.leaderboard.api.port.in.ProjectLeaderboardUseCase;
import com.cryptostrategy.platform.leaderboard.api.port.out.LeaderboardStore;
import com.cryptostrategy.platform.leaderboard.internal.LeaderboardReconciliationService;
import com.cryptostrategy.platform.leaderboard.internal.LeaderboardQueryService;
import com.cryptostrategy.platform.leaderboard.internal.LeaderboardService;

public final class LeaderboardModuleFactory {
    private LeaderboardModuleFactory() {}

    public static ProjectLeaderboardUseCase projectLeaderboardUseCase(LeaderboardStore store) {
        return new LeaderboardService(store);
    }

    public static GetLeaderboardUseCase getLeaderboardUseCase(LeaderboardStore store) {
        return new LeaderboardQueryService(store);
    }

    public static LeaderboardReconciliationUseCase leaderboardReconciliationUseCase(
            LeaderboardStore store,
            ProjectLeaderboardUseCase projectLeaderboardUseCase
    ) {
        return new LeaderboardReconciliationService(store, projectLeaderboardUseCase);
    }
}

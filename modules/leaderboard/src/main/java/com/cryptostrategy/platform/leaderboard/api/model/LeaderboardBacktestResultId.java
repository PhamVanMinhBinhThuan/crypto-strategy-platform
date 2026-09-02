package com.cryptostrategy.platform.leaderboard.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

/** Backtest-result identity as exposed by the leaderboard projection boundary. */
public record LeaderboardBacktestResultId(String value) implements UlidIdentifier {
    public LeaderboardBacktestResultId {
        value = Ulids.requireValid(value);
    }
}

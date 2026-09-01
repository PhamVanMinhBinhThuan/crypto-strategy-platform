package com.cryptostrategy.platform.execution.api;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevision;
import java.util.Objects;
public record ExecutionEvidence(BacktestResult backtest, EvaluationResult evaluation, LeaderboardRevision leaderboard) {
    public ExecutionEvidence { Objects.requireNonNull(backtest);Objects.requireNonNull(evaluation);Objects.requireNonNull(leaderboard); }
}

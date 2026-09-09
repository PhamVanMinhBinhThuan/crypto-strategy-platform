package com.cryptostrategy.platform.execution.api;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevision;
import java.util.Objects;
import java.util.List;
public record ExecutionEvidence(BacktestResult backtest, EvaluationResult evaluation,
        LeaderboardRevision leaderboard, List<String> orderedCandidateFingerprints) {
    public ExecutionEvidence(BacktestResult backtest, EvaluationResult evaluation,
            LeaderboardRevision leaderboard) {
        this(backtest, evaluation, leaderboard, List.of());
    }
    public ExecutionEvidence {
        Objects.requireNonNull(backtest); Objects.requireNonNull(evaluation); Objects.requireNonNull(leaderboard);
        orderedCandidateFingerprints = List.copyOf(orderedCandidateFingerprints);
    }
}

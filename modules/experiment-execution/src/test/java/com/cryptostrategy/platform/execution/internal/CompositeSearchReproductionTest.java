package com.cryptostrategy.platform.execution.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.execution.api.ExecutionEvidence;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevision;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompositeSearchReproductionTest {
    @Test
    void comparesTheCompleteOrderedCompositeCandidateFingerprintSequence() {
        var source = evidence(List.of("candidate-a", "candidate-b"));
        var exact = evidence(List.of("candidate-a", "candidate-b"));
        var reordered = evidence(List.of("candidate-b", "candidate-a"));

        assertThat(SearchReproductionVerificationCoordinator.compare(source, exact).matches()).isTrue();
        var mismatch = SearchReproductionVerificationCoordinator.compare(source, reordered);
        assertThat(mismatch.matches()).isFalse();
        assertThat(mismatch.differences()).containsKey("fingerprints");
    }

    private static ExecutionEvidence evidence(List<String> candidates) {
        BacktestResult backtest = mock(BacktestResult.class);
        EvaluationResult evaluation = mock(EvaluationResult.class);
        LeaderboardRevision leaderboard = mock(LeaderboardRevision.class);
        when(backtest.trades()).thenReturn(List.of());
        when(backtest.fingerprint()).thenReturn("backtest");
        when(evaluation.totalReturn()).thenReturn(BigDecimal.ONE);
        when(evaluation.winRate()).thenReturn(BigDecimal.ONE);
        when(evaluation.maximumDrawdown()).thenReturn(BigDecimal.ZERO);
        when(evaluation.numberOfTrades()).thenReturn(0);
        when(evaluation.fingerprint()).thenReturn("evaluation");
        when(leaderboard.fingerprint()).thenReturn("leaderboard");
        return new ExecutionEvidence(backtest, evaluation, leaderboard, candidates);
    }
}

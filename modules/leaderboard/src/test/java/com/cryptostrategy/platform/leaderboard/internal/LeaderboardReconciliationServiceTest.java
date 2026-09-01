package com.cryptostrategy.platform.leaderboard.internal;

import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import com.cryptostrategy.platform.evaluation.api.model.MetricVersion;
import com.cryptostrategy.platform.evaluation.api.model.RankingVersion;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevision;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevisionId;
import com.cryptostrategy.platform.leaderboard.api.port.in.ProjectLeaderboardUseCase;
import com.cryptostrategy.platform.leaderboard.api.port.out.LeaderboardStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeaderboardReconciliationServiceTest {

    private LeaderboardStore store;
    private ProjectLeaderboardUseCase projectLeaderboardUseCase;
    private LeaderboardReconciliationService service;

    private final ExperimentId experimentId = ExperimentId.generate();

    @BeforeEach
    void setUp() {
        store = mock(LeaderboardStore.class);
        projectLeaderboardUseCase = mock(ProjectLeaderboardUseCase.class);
        service = new LeaderboardReconciliationService(store, projectLeaderboardUseCase);
    }

    @Test
    void reconcilesEvaluationsIntoLeaderboardRevision() {
        Instant now = Instant.now();
        EvaluationResult eval = new EvaluationResult(
                EvaluationResultId.generate(), experimentId,
                com.cryptostrategy.platform.backtesting.api.model.BacktestResultId.generate(),
                new MetricVersion("metric-v1"), new RankingVersion("ranking-v1"),
                BigDecimal.valueOf(0.15), BigDecimal.valueOf(0.55), BigDecimal.valueOf(0.10),
                20, BigDecimal.valueOf(0.75), true, "fp", now
        );
        when(store.listEvaluationsForExperiment(experimentId, 20)).thenReturn(List.of(eval));

        LeaderboardRevision expectedRevision = new LeaderboardRevision(
                LeaderboardRevisionId.generate(), experimentId, 1L, 10, new RankingVersion("ranking-v1"),
                List.of(), "rev-fp", now
        );
        when(projectLeaderboardUseCase.project(eq(experimentId), any(), eq(10), eq(List.of(eval))))
                .thenReturn(expectedRevision);

        Optional<LeaderboardRevision> result = service.reconcileLeaderboard(experimentId, 20);
        assertThat(result).contains(expectedRevision);
        verify(projectLeaderboardUseCase).project(eq(experimentId), any(), eq(10), eq(List.of(eval)));
    }

    @Test
    void returnsEmptyWhenNoEvaluationsExist() {
        when(store.listEvaluationsForExperiment(experimentId, 20)).thenReturn(List.of());
        Optional<LeaderboardRevision> result = service.reconcileLeaderboard(experimentId, 20);
        assertThat(result).isEmpty();
    }
}

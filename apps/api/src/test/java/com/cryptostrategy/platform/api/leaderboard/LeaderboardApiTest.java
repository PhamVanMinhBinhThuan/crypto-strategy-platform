package com.cryptostrategy.platform.api.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import com.cryptostrategy.platform.evaluation.api.model.RankingVersion;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevisionId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardSnapshot;
import com.cryptostrategy.platform.leaderboard.api.port.in.GetLeaderboardUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LeaderboardApiTest {
    @Test
    void exposesRevisionRankingPolicyAndDeterministicBoundedOrder() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant now = Instant.parse("2026-09-02T00:00:00Z");
        ExperimentId experimentId = new ExperimentId("01J00000000000000000000001");
        var revisionId = new LeaderboardRevisionId("01J00000000000000000000002");
        GetExperimentUseCase experiments = mock(GetExperimentUseCase.class);
        GetLeaderboardUseCase leaderboards = mock(GetLeaderboardUseCase.class);
        when(experiments.getExperiment(owner, experimentId)).thenReturn(Optional.of(
                Experiment.create(experimentId, owner, "search", null, null, now)));
        when(leaderboards.getLatest(experimentId)).thenReturn(Optional.of(new LeaderboardSnapshot(
                revisionId,
                experimentId,
                7,
                2,
                new RankingVersion("ranking-v1"),
                List.of(entry(1, "03", "04", "0.90"), entry(2, "05", "06", "0.80")),
                "sha256:" + "a".repeat(64),
                now)));
        var controller = new LeaderboardController(
                experiments, leaderboards, new PageRequestMapper());

        var response = controller.getLeaderboard(
                new AuthenticatedUserContext(owner, now.plusSeconds(60)),
                experimentId.value(),
                1,
                null);

        assertThat(response.revision()).isEqualTo(7);
        assertThat(response.rankingPolicyVersion()).isEqualTo("ranking-v1");
        assertThat(response.items()).extracting(LeaderboardDtos.EntryResponse::rank)
                .containsExactly(1);
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isNotBlank();
    }

    private static LeaderboardSnapshot.Entry entry(
            int rank, String evaluationSuffix, String resultSuffix, String score) {
        return new LeaderboardSnapshot.Entry(
                rank,
                new EvaluationResultId("01J000000000000000000000" + evaluationSuffix),
                new com.cryptostrategy.platform.leaderboard.api.model.LeaderboardBacktestResultId(
                        "01J000000000000000000000" + resultSuffix),
                new BigDecimal(score),
                new BigDecimal("0.10"),
                "sha256:" + Integer.toHexString(rank).repeat(64));
    }
}

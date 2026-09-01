package com.cryptostrategy.platform.leaderboard.internal;

import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.evaluation.api.model.RankingVersion;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevision;
import com.cryptostrategy.platform.leaderboard.api.port.in.LeaderboardReconciliationUseCase;
import com.cryptostrategy.platform.leaderboard.api.port.in.ProjectLeaderboardUseCase;
import com.cryptostrategy.platform.leaderboard.api.port.out.LeaderboardStore;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class LeaderboardReconciliationService implements LeaderboardReconciliationUseCase {

    private final LeaderboardStore store;
    private final ProjectLeaderboardUseCase projectLeaderboardUseCase;

    public LeaderboardReconciliationService(
            LeaderboardStore store,
            ProjectLeaderboardUseCase projectLeaderboardUseCase
    ) {
        this.store = Objects.requireNonNull(store, "store cannot be null");
        this.projectLeaderboardUseCase = Objects.requireNonNull(projectLeaderboardUseCase, "projectLeaderboardUseCase cannot be null");
    }

    @Override
    public Optional<LeaderboardRevision> reconcileLeaderboard(ExperimentId experimentId, int batchSize) {
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        int boundedBatch = Math.max(1, batchSize);

        List<EvaluationResult> evaluations = store.listEvaluationsForExperiment(experimentId, boundedBatch);
        if (evaluations.isEmpty()) {
            return Optional.empty();
        }

        RankingVersion rankingVersion = new RankingVersion("ranking-v1");
        int topK = 10;
        LeaderboardRevision revision = projectLeaderboardUseCase.project(experimentId, rankingVersion, topK, evaluations);
        return Optional.ofNullable(revision);
    }
}

package com.cryptostrategy.platform.leaderboard.api.port.out;

import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevision;

import java.util.List;
import java.util.Optional;

public interface LeaderboardStore {
    Optional<LeaderboardRevision> latest(ExperimentId experimentId);
    LeaderboardRevision save(LeaderboardRevision revision);
    List<EvaluationResult> listEvaluationsForExperiment(ExperimentId experimentId, int limit);
}

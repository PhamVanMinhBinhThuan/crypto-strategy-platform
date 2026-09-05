package com.cryptostrategy.platform.leaderboard.api.port.out;

import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevision;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardCandidateEvidence;

import java.util.List;
import java.util.Optional;

public interface LeaderboardStore {
    Optional<LeaderboardRevision> latest(ExperimentId experimentId);
    LeaderboardRevision save(LeaderboardRevision revision);
    List<EvaluationResult> listEvaluationsForExperiment(ExperimentId experimentId, int limit);
    default Optional<EvaluationResult> findEvaluation(
            com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId evaluationResultId) {
        return Optional.empty();
    }
    default Optional<LeaderboardCandidateEvidence> findCandidateEvidence(
            ExperimentId experimentId, CandidateId candidateId) {
        return Optional.empty();
    }

    default Optional<LeaderboardCandidateEvidence> findCandidateEvidence(
            ExperimentId experimentId,
            com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId evaluationResultId) {
        return Optional.empty();
    }
}

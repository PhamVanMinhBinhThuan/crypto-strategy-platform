package com.cryptostrategy.platform.leaderboard.api.model;

import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import java.util.Map;
import java.util.Objects;

/** Immutable candidate/backtest/evaluation join used only for authoritative reads. */
public record LeaderboardCandidateEvidence(
        ExperimentId experimentId,
        CandidateId candidateId,
        int generationIndex,
        Map<String, Object> definition,
        Map<String, Object> generatorState,
        String candidateFingerprint,
        LeaderboardBacktestResultId backtestResultId,
        String backtestStatus,
        EvaluationResult evaluation) {
    public LeaderboardCandidateEvidence {
        Objects.requireNonNull(experimentId, "experimentId");
        Objects.requireNonNull(candidateId, "candidateId");
        if (generationIndex < 0) throw new IllegalArgumentException("generationIndex");
        definition = Map.copyOf(Objects.requireNonNull(definition, "definition"));
        generatorState = generatorState == null ? Map.of() : Map.copyOf(generatorState);
        candidateFingerprint = requireText(candidateFingerprint, "candidateFingerprint");
        backtestStatus = requireText(backtestStatus, "backtestStatus");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}

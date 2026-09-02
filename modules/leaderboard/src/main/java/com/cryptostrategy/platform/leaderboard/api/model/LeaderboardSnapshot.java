package com.cryptostrategy.platform.leaderboard.api.model;

import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import com.cryptostrategy.platform.evaluation.api.model.RankingVersion;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Immutable read projection for one durable Leaderboard revision. */
public record LeaderboardSnapshot(
        LeaderboardRevisionId revisionId,
        ExperimentId experimentId,
        long revisionNumber,
        int topK,
        RankingVersion rankingVersion,
        List<Entry> entries,
        String fingerprint,
        Instant createdAt) {
    public LeaderboardSnapshot {
        entries = List.copyOf(entries);
    }

    public record Entry(
            int rank,
            EvaluationResultId evaluationResultId,
            LeaderboardBacktestResultId backtestResultId,
            BigDecimal score,
            BigDecimal maximumDrawdown,
            String evaluationFingerprint) {
        public Entry {
            Objects.requireNonNull(evaluationResultId, "evaluationResultId");
            Objects.requireNonNull(backtestResultId, "backtestResultId");
            Objects.requireNonNull(score, "score");
            Objects.requireNonNull(maximumDrawdown, "maximumDrawdown");
            Objects.requireNonNull(evaluationFingerprint, "evaluationFingerprint");
        }
    }
}

package com.cryptostrategy.platform.api.leaderboard;

import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardSnapshot;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevisionId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardBacktestResultId;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.api.transport.TypedUlidSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import java.util.List;

public final class LeaderboardDtos {
    private LeaderboardDtos() {}

    public record LeaderboardResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId experimentId,
            @JsonSerialize(using = TypedUlidSerializer.class) LeaderboardRevisionId revisionId,
            long revision,
            int topK,
            String rankingPolicyVersion,
            String fingerprint,
            Instant createdAt,
            List<EntryResponse> items,
            String nextCursor,
            boolean hasMore) {}

    public record EntryResponse(
            int rank,
            @JsonSerialize(using = TypedUlidSerializer.class) EvaluationResultId evaluationResultId,
            @JsonSerialize(using = TypedUlidSerializer.class) LeaderboardBacktestResultId backtestResultId,
            String score,
            String maximumDrawdown,
            String evaluationFingerprint) {
        static EntryResponse from(LeaderboardSnapshot.Entry entry) {
            return new EntryResponse(
                    entry.rank(),
                    entry.evaluationResultId(),
                    entry.backtestResultId(),
                    entry.score().toPlainString(),
                    entry.maximumDrawdown().toPlainString(),
                    entry.evaluationFingerprint());
        }
    }
}

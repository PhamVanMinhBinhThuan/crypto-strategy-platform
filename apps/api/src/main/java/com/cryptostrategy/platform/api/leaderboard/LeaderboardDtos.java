package com.cryptostrategy.platform.api.leaderboard;

import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardSnapshot;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevisionId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardBacktestResultId;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.api.transport.TypedUlidSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardCandidateEvidence;

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
            String evaluationFingerprint,
            @JsonSerialize(using = TypedUlidSerializer.class) CandidateId candidateId,
            String candidateFingerprint,
            String candidateSummary,
            MetricsResponse metrics) {
        static EntryResponse from(LeaderboardSnapshot.Entry entry) {
            return new EntryResponse(
                    entry.rank(),
                    entry.evaluationResultId(),
                    entry.backtestResultId(),
                    entry.score().toPlainString(),
                    entry.maximumDrawdown().toPlainString(),
                    entry.evaluationFingerprint(), entry.candidateId(), entry.candidateFingerprint(),
                    summary(entry.candidateDefinition()),
                    entry.totalReturn() == null ? null : new MetricsResponse(
                            entry.totalReturn().toPlainString(), entry.winRate().toPlainString(),
                            entry.maximumDrawdown().toPlainString(), entry.numberOfTrades(),
                            entry.metricVersion()));
        }
    }

    public record MetricsResponse(String totalReturn, String winRate,
            String maximumDrawdown, int numberOfTrades, String metricVersion) {}

    public record DatasetProvenanceResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) DatasetVersionId datasetId,
            String version, String checksum,
            String provider, String pair, String timeframe, String normalizationVersion,
            Instant startTime, Instant endTime, long candleCount) {
        static DatasetProvenanceResponse from(DatasetProvenanceSnapshot value) {
            return new DatasetProvenanceResponse(value.datasetVersionId(), value.version(),
                    value.checksum(), value.provider(), value.tradingPair(), value.timeframe(),
                    value.normalizationVersion(), value.rangeStart(), value.rangeEnd(), value.candleCount());
        }
    }

    public record CandidateDetailResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) CandidateId candidateId,
            int generationIndex,
            Map<String, Object> definition, Map<String, Object> generatorState,
            String candidateFingerprint, DatasetProvenanceResponse dataset,
            @JsonSerialize(using = TypedUlidSerializer.class)
            LeaderboardBacktestResultId backtestResultId,
            String backtestStatus, MetricsResponse metrics) {
        public static CandidateDetailResponse from(LeaderboardCandidateEvidence value,
                DatasetProvenanceSnapshot dataset) {
            var evaluation = value.evaluation();
            return new CandidateDetailResponse(value.candidateId(), value.generationIndex(),
                    value.definition(), value.generatorState(), value.candidateFingerprint(),
                    DatasetProvenanceResponse.from(dataset), value.backtestResultId(),
                    value.backtestStatus(), evaluation == null ? null : new MetricsResponse(
                            evaluation.totalReturn().toPlainString(), evaluation.winRate().toPlainString(),
                            evaluation.maximumDrawdown().toPlainString(), evaluation.numberOfTrades(),
                            evaluation.metricVersion().value()));
        }

        public static CandidateDetailResponse from(
                com.cryptostrategy.platform.experiment.api.CandidateDefinition value,
                DatasetProvenanceSnapshot dataset) {
            return new CandidateDetailResponse(value.candidateId(), value.generationIndex(),
                    value.definition(), value.generatorState(), value.fingerprint(),
                    DatasetProvenanceResponse.from(dataset), null, "QUEUED", null);
        }
    }

    @SuppressWarnings("unchecked")
    private static String summary(Map<String, Object> definition) {
        if (definition == null || definition.isEmpty()) return null;
        Object components = definition.get("components");
        if (components instanceof List<?> list) {
            String result = list.stream().filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(component -> String.valueOf(component.getOrDefault("strategyId", "Strategy")))
                    .collect(java.util.stream.Collectors.joining(" + "));
            return result.isBlank() ? null : result;
        }
        Object strategy = definition.get("strategyId");
        return strategy == null ? null : strategy.toString();
    }
}

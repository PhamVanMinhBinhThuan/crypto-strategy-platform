package com.cryptostrategy.platform.api.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import com.cryptostrategy.platform.evaluation.api.model.MetricVersion;
import com.cryptostrategy.platform.evaluation.api.model.RankingVersion;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardBacktestResultId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardCandidateEvidence;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CompositeLeaderboardApiTest {
    private static final Instant NOW = Instant.parse("2026-09-05T00:00:00Z");
    private static final ExperimentId EXPERIMENT =
            new ExperimentId("01J00000000000000000000501");

    @Test
    void exposesCompositeIdentityAndOnlyTheFourReleasedMetrics() {
        var entry = new LeaderboardSnapshot.Entry(1,
                new EvaluationResultId("01J00000000000000000000502"),
                new LeaderboardBacktestResultId("01J00000000000000000000503"),
                new BigDecimal("0.84"), new BigDecimal("0.12"), "sha256:" + "a".repeat(64),
                new CandidateId("01J00000000000000000000504"), "sha256:" + "b".repeat(64),
                Map.of("schemaVersion", 2, "components", List.of(
                        Map.of("strategyId", "ma-crossover"), Map.of("strategyId", "rsi"))),
                new BigDecimal("1.425"), new BigDecimal("0.582"), 1245, "metric-v1");

        var response = LeaderboardDtos.EntryResponse.from(entry);

        assertThat(response.candidateSummary()).isEqualTo("ma-crossover + rsi");
        assertThat(response.candidateFingerprint()).isEqualTo("sha256:" + "b".repeat(64));
        assertThat(response.metrics()).isEqualTo(new LeaderboardDtos.MetricsResponse(
                "1.425", "0.582", "0.12", 1245, "metric-v1"));
        assertThat(response.toString()).doesNotContainIgnoringCase("sharpe");
    }

    @Test
    void candidateDetailTracesDefinitionDatasetBacktestAndMetricVersion() {
        EvaluationResult evaluation = new EvaluationResult(
                new EvaluationResultId("01J00000000000000000000502"), EXPERIMENT,
                new BacktestResultId("01J00000000000000000000503"),
                new MetricVersion("metric-v1"), new RankingVersion("ranking-v1"),
                new BigDecimal("0.42"), new BigDecimal("0.58"), new BigDecimal("0.12"),
                24, new BigDecimal("0.84"), true, "sha256:" + "c".repeat(64), NOW);
        var evidence = new LeaderboardCandidateEvidence(EXPERIMENT,
                new CandidateId("01J00000000000000000000504"), 7,
                Map.of("schemaVersion", 2, "kind", "COMPOSITE"), Map.of("cursor", 8),
                "sha256:" + "b".repeat(64),
                new LeaderboardBacktestResultId("01J00000000000000000000503"),
                "SUCCEEDED", evaluation);
        var dataset = new DatasetProvenanceSnapshot(
                new DatasetVersionId("01J00000000000000000000505"), "candle-v1",
                "sha256:" + "d".repeat(64), "BINANCE", "BTC/USDT", "1h", "binance-v1",
                NOW.minusSeconds(3600), NOW, 1);

        var response = LeaderboardDtos.CandidateDetailResponse.from(evidence, dataset);

        assertThat(response.candidateFingerprint()).isEqualTo(evidence.candidateFingerprint());
        assertThat(response.dataset().checksum()).isEqualTo(dataset.checksum());
        assertThat(response.backtestResultId()).isEqualTo(evidence.backtestResultId());
        assertThat(response.metrics().metricVersion()).isEqualTo("metric-v1");
    }
}

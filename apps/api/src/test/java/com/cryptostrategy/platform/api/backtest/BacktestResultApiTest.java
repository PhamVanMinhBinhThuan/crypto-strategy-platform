package com.cryptostrategy.platform.api.backtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.backtesting.api.model.BacktestAssumptions;
import com.cryptostrategy.platform.backtesting.api.model.BacktestProvenance;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.backtesting.api.model.EquityCurveSummary;
import com.cryptostrategy.platform.backtesting.api.model.Money;
import com.cryptostrategy.platform.backtesting.api.port.in.GetBacktestResultUseCase;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.backtest.BacktestId;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktest;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.port.in.GetStandaloneBacktestUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BacktestResultApiTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-02T00:00:00Z");
    private static final BacktestId BACKTEST_ID = new BacktestId("01J00000000000000000000001");
    private static final ExperimentId EXPERIMENT_ID = new ExperimentId("01J00000000000000000000002");
    private static final CandidateId CANDIDATE_ID = new CandidateId("01J00000000000000000000003");
    private static final JobId JOB_ID = new JobId("01J00000000000000000000004");

    @Test
    void preservesExactMetricsUtcAndProvenance() {
        GetStandaloneBacktestUseCase backtests = mock(GetStandaloneBacktestUseCase.class);
        GetBacktestResultUseCase results = mock(GetBacktestResultUseCase.class);
        when(backtests.getStandaloneBacktest(OWNER, BACKTEST_ID)).thenReturn(Optional.of(
                new StandaloneBacktest(BACKTEST_ID, EXPERIMENT_ID, CANDIDATE_ID, JOB_ID, NOW)));
        when(results.getByJobId(JOB_ID)).thenReturn(Optional.of(result()));
        var controller = new BacktestResultController(backtests, results);

        var response = controller.getResult(
                new AuthenticatedUserContext(OWNER, NOW.plusSeconds(60)), BACKTEST_ID.value());

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.initialCapital()).isEqualTo("10000.000000000000");
        assertThat(response.metrics().totalReturn()).isEqualTo("0.050000000000");
        assertThat(response.provenance().manifestFingerprint()).startsWith("sha256:");
        assertThat(response.completedAt()).isEqualTo(NOW);
    }

    @Test
    void concealsMissingAndForeignBacktests() {
        var controller = new BacktestResultController(
                mock(GetStandaloneBacktestUseCase.class), mock(GetBacktestResultUseCase.class));

        assertThatThrownBy(() -> controller.getResult(
                        new AuthenticatedUserContext(OWNER, NOW.plusSeconds(60)),
                        BACKTEST_ID.value()))
                .isInstanceOf(ResourceInaccessibleException.class);
    }

    @Test
    void readsAnOwnedResultByItsCanonicalResultIdWithoutInventingABacktestId() {
        GetBacktestResultUseCase results = mock(GetBacktestResultUseCase.class);
        GetExperimentUseCase experiments = mock(GetExperimentUseCase.class);
        ListCandidatesUseCase candidates = mock(ListCandidatesUseCase.class);
        var result = result();
        when(results.getByResultId(result.resultId())).thenReturn(Optional.of(result));
        when(experiments.getExperiment(OWNER, EXPERIMENT_ID)).thenReturn(Optional.of(mock(
                com.cryptostrategy.platform.experiment.api.Experiment.class)));
        when(experiments.getManifest(OWNER, EXPERIMENT_ID)).thenReturn(Optional.of(manifest()));
        when(candidates.getCandidate(OWNER, EXPERIMENT_ID, CANDIDATE_ID))
                .thenReturn(Optional.of(candidate()));
        var controller = new BacktestResultByIdController(results, experiments, candidates);

        var response = controller.getResult(
                new AuthenticatedUserContext(OWNER, NOW.plusSeconds(60)), result.resultId().value());

        assertThat(response.backtestResultId()).isEqualTo(result.resultId());
        assertThat(response.backtestId()).isNull();
        assertThat(response.provenance().dataset().datasetVersionId().value())
                .isEqualTo("01J00000000000000000000008");
        assertThat(response.provenance().dataset().version()).isEqualTo("binance-btcusdt-1h-v1");
        assertThat(response.provenance().dataset().checksum()).isEqualTo(fingerprint('b'));
        assertThat(response.provenance().strategy().singleStrategy().pluginId())
                .extracting(StrategyPluginId::value)
                .isEqualTo("moving-average-crossover");
        assertThat(response.provenance().strategy().singleStrategy().implementationVersion())
                .isEqualTo("1.0.0");
        assertThat(response.provenance().strategy().parameters()).containsEntry(
                "fastPeriod", new ResultDtos.ParameterEvidenceResponse("INTEGER", "20"));
        assertThat(response.provenance().candidate().candidateId()).isEqualTo(CANDIDATE_ID);
        assertThat(response.provenance().candidate().definition())
                .containsEntry("fastPeriod", 20L)
                .containsEntry("slowPeriod", 50L);
        assertThat(response.provenance().successfulAttemptId().value())
                .isEqualTo("01J00000000000000000000007");
        assertThat(response.provenance().softwareVersion()).isEqualTo("f014-demo");
        assertThat(response.provenance().gitCommit()).isEqualTo("50c28d9");
        assertThat(response.assumptions()).containsEntry("assumptionsVersion", "backtest-assumptions-v1");
    }

    @Test
    void concealsAResultWhenItsExperimentIsNotOwned() {
        GetBacktestResultUseCase results = mock(GetBacktestResultUseCase.class);
        var result = result();
        when(results.getByResultId(result.resultId())).thenReturn(Optional.of(result));
        var controller = new BacktestResultByIdController(
                results, mock(GetExperimentUseCase.class), mock(ListCandidatesUseCase.class));

        assertThatThrownBy(() -> controller.getResult(
                        new AuthenticatedUserContext(OWNER, NOW.plusSeconds(60)),
                        result.resultId().value()))
                .isInstanceOf(ResourceInaccessibleException.class);
    }

    private static BacktestResult result() {
        return new BacktestResult(
                new BacktestResultId("01J00000000000000000000006"),
                EXPERIMENT_ID,
                CANDIDATE_ID,
                JOB_ID,
                new AttemptId("01J00000000000000000000007"),
                new BacktestProvenance(fingerprint('a'), fingerprint('b'), fingerprint('c')),
                BacktestAssumptions.mvp(
                        new BigDecimal("10000"), new BigDecimal("0.001"), BigDecimal.ZERO),
                Money.of(new BigDecimal("10000")),
                Money.of(new BigDecimal("10500")),
                Money.of(new BigDecimal("10")),
                List.of(),
                new EquityCurveSummary(
                        1,
                        Money.of(new BigDecimal("10500")),
                        Money.of(new BigDecimal("10500")),
                        0,
                        0,
                        fingerprint('d')),
                fingerprint('e'),
                NOW);
    }

    private static ExperimentManifest manifest() {
        var reference = new StrategyReference(
                new StrategyVersionId("01J00000000000000000000009"),
                new StrategyPluginId("moving-average-crossover"), SemanticVersion.parse("1.0.0"));
        var parameters = StrategyParameterSet.of(java.util.Map.of(
                "fastPeriod", new StrategyParameterValue.IntegerValue(20),
                "slowPeriod", new StrategyParameterValue.IntegerValue(50)));
        return new ExperimentManifest(
                EXPERIMENT_ID,
                "experiment-manifest-v1",
                new DatasetProvenanceSnapshot(
                        new DatasetVersionId("01J00000000000000000000008"),
                        "binance-btcusdt-1h-v1",
                        fingerprint('b'),
                        "binance",
                        "BTCUSDT",
                        "1h",
                        "ohlcv-v1",
                        NOW.minusSeconds(3600),
                        NOW,
                        60),
                StrategyProvenanceSnapshot.single(
                        reference, parameters, Optional.empty(), "strategy-v1:" + fingerprint('c')),
                java.util.Map.of("feeRate", "0.001"),
                java.util.Map.of("maximumCandidates", 10),
                java.util.Map.of("rankingVersion", "ranking-v1"),
                null,
                "f014-demo",
                "50c28d9",
                fingerprint('a'),
                NOW.minusSeconds(10));
    }

    private static CandidateDefinition candidate() {
        return new CandidateDefinition(
                CANDIDATE_ID,
                EXPERIMENT_ID,
                0,
                java.util.Map.of("fastPeriod", 20L, "slowPeriod", 50L),
                java.util.Map.of("drawIndex", 1),
                fingerprint('f'),
                NOW.minusSeconds(5));
    }

    private static String fingerprint(char value) {
        return "sha256:" + String.valueOf(value).repeat(64);
    }
}

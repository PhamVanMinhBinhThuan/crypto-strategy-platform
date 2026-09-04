package com.cryptostrategy.platform.api.experiment;

import static com.cryptostrategy.platform.api.support.AuthenticatedUsers.USER_A_ID;
import static com.cryptostrategy.platform.api.support.AuthenticatedUsers.authenticatedAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptostrategy.platform.backtesting.api.model.BacktestAssumptions;
import com.cryptostrategy.platform.backtesting.api.model.BacktestProvenance;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.backtesting.api.model.EquityCurveSummary;
import com.cryptostrategy.platform.backtesting.api.model.Money;
import com.cryptostrategy.platform.backtesting.api.port.in.GetBacktestResultUseCase;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import com.cryptostrategy.platform.evaluation.api.model.MetricVersion;
import com.cryptostrategy.platform.evaluation.api.model.RankingVersion;
import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardBacktestResultId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevisionId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardSnapshot;
import com.cryptostrategy.platform.leaderboard.api.port.in.GetLeaderboardUseCase;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:f014-research-flow;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=fixture-password",
            "platform.security.jwt.issuer=https://fixture.invalid/auth/v1",
            "platform.security.jwt.jwk-set-uri=https://fixture.invalid/.well-known/jwks.json",
            "platform.security.jwt.audience=authenticated"
        })
@AutoConfigureMockMvc
class F014ResearchFlowIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-09-04T02:00:00Z");
    private static final ExperimentId EXPERIMENT_ID =
            new ExperimentId("01J00000000000000000000301");
    private static final CandidateId CANDIDATE_ID =
            new CandidateId("01J00000000000000000000302");
    private static final JobId JOB_ID = new JobId("01J00000000000000000000303");
    private static final AttemptId ATTEMPT_ID =
            new AttemptId("01J00000000000000000000304");
    private static final BacktestResultId RESULT_ID =
            new BacktestResultId("01J00000000000000000000305");
    private static final EvaluationResultId EVALUATION_ID =
            new EvaluationResultId("01J00000000000000000000306");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private GetExperimentUseCase experiments;

    @MockitoBean
    private ListCandidatesUseCase candidates;

    @MockitoBean
    private GetBacktestResultUseCase results;

    @MockitoBean
    private GetLeaderboardUseCase leaderboards;

    private CandidateDefinition candidate;
    private BacktestResult result;
    private EvaluationResult evaluation;

    @BeforeEach
    void setUpIdentityChain() {
        candidate = new CandidateDefinition(
                CANDIDATE_ID,
                EXPERIMENT_ID,
                0,
                Map.of("fastPeriod", 5, "slowPeriod", 25),
                Map.of("seed", 20260904),
                fingerprint('a'),
                NOW);
        result = result();
        evaluation = new EvaluationResult(
                EVALUATION_ID,
                EXPERIMENT_ID,
                RESULT_ID,
                new MetricVersion("metric-v1"),
                new RankingVersion("ranking-v1"),
                new BigDecimal("0.05"),
                new BigDecimal("0.50"),
                new BigDecimal("0.10"),
                1,
                new BigDecimal("0.80"),
                true,
                fingerprint('f'),
                NOW.plusSeconds(1));
        LeaderboardSnapshot board = new LeaderboardSnapshot(
                new LeaderboardRevisionId("01J00000000000000000000307"),
                EXPERIMENT_ID,
                1,
                10,
                evaluation.rankingVersion(),
                List.of(new LeaderboardSnapshot.Entry(
                        1,
                        evaluation.evaluationResultId(),
                        new LeaderboardBacktestResultId(evaluation.backtestResultId().value()),
                        evaluation.overallScore(),
                        evaluation.maximumDrawdown(),
                        evaluation.fingerprint())),
                fingerprint('b'),
                NOW.plusSeconds(2));

        when(experiments.getExperiment(USER_A_ID, EXPERIMENT_ID))
                .thenReturn(Optional.of(
                        Experiment.create(EXPERIMENT_ID, USER_A_ID, "F014", null, null, NOW)));
        when(experiments.getManifest(USER_A_ID, EXPERIMENT_ID))
                .thenReturn(Optional.of(manifest()));
        when(candidates.listCandidates(USER_A_ID, EXPERIMENT_ID, -1, "", 11))
                .thenReturn(List.of(candidate));
        when(candidates.getCandidate(USER_A_ID, EXPERIMENT_ID, CANDIDATE_ID))
                .thenReturn(Optional.of(candidate));
        when(results.getByResultId(RESULT_ID)).thenReturn(Optional.of(result));
        when(leaderboards.getLatest(EXPERIMENT_ID)).thenReturn(Optional.of(board));
    }

    @Test
    void preservesCandidateResultEvaluationAndLeaderboardIdentityAcrossPublicReads()
            throws Exception {
        JsonNode candidatePage = body(get("/api/v1/experiments/{id}/candidates", EXPERIMENT_ID.value())
                .queryParam("limit", "10"));
        JsonNode leaderboard = body(get("/api/v1/experiments/{id}/leaderboard", EXPERIMENT_ID.value())
                .queryParam("limit", "10"));
        JsonNode resultResponse = body(get("/api/v1/backtest-results/{id}", RESULT_ID.value()));

        assertThat(candidatePage.path("items").get(0).path("candidateId").asText())
                .isEqualTo(CANDIDATE_ID.value());
        assertThat(leaderboard.path("experimentId").asText()).isEqualTo(EXPERIMENT_ID.value());
        assertThat(leaderboard.path("items").get(0).path("evaluationResultId").asText())
                .isEqualTo(evaluation.evaluationResultId().value());
        assertThat(leaderboard.path("items").get(0).path("backtestResultId").asText())
                .isEqualTo(RESULT_ID.value());
        assertThat(resultResponse.path("backtestResultId").asText()).isEqualTo(RESULT_ID.value());
        assertThat(resultResponse.path("provenance").path("experimentId").asText())
                .isEqualTo(EXPERIMENT_ID.value());
        assertThat(resultResponse.path("provenance").path("candidateId").asText())
                .isEqualTo(CANDIDATE_ID.value());
        assertThat(evaluation.experimentId()).isEqualTo(EXPERIMENT_ID);
        assertThat(evaluation.backtestResultId()).isEqualTo(result.resultId());
    }

    private JsonNode body(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        String content = mockMvc.perform(request.with(authenticatedAs(USER_A_ID)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.readTree(content);
    }

    private static BacktestResult result() {
        return new BacktestResult(
                RESULT_ID,
                EXPERIMENT_ID,
                CANDIDATE_ID,
                JOB_ID,
                ATTEMPT_ID,
                new BacktestProvenance(fingerprint('c'), fingerprint('d'), fingerprint('e')),
                BacktestAssumptions.mvp(
                        new BigDecimal("10000"), new BigDecimal("0.001"), BigDecimal.ZERO),
                Money.of(new BigDecimal("10000")),
                Money.of(new BigDecimal("10500")),
                Money.of(new BigDecimal("10")),
                List.of(),
                new EquityCurveSummary(
                        1,
                        Money.of(new BigDecimal("10500")),
                        Money.of(new BigDecimal("9450")),
                        0,
                        0,
                        fingerprint('9')),
                fingerprint('8'),
                NOW);
    }

    private static ExperimentManifest manifest() {
        StrategyReference reference = new StrategyReference(
                new StrategyVersionId("01J00000000000000000000308"),
                new StrategyPluginId("ma-crossover"),
                SemanticVersion.parse("1.0.0"));
        StrategyParameterSet parameters = StrategyParameterSet.of(Map.of(
                "fastPeriod", new StrategyParameterValue.IntegerValue(5),
                "slowPeriod", new StrategyParameterValue.IntegerValue(25)));
        return new ExperimentManifest(
                EXPERIMENT_ID,
                "experiment-manifest-v1",
                new DatasetProvenanceSnapshot(
                        new DatasetVersionId("01J00000000000000000000309"),
                        "f014-dataset-v1",
                        fingerprint('d'),
                        "binance",
                        "BTCUSDT",
                        "1h",
                        "ohlcv-v1",
                        NOW.minusSeconds(3600),
                        NOW,
                        60),
                StrategyProvenanceSnapshot.single(
                        reference, parameters, Optional.empty(), "strategy-v1:" + fingerprint('e')),
                Map.of("feeRate", "0.001"),
                Map.of("maximumCandidates", 1),
                Map.of("rankingVersion", "ranking-v1"),
                null,
                "f014-demo",
                "50c28d9",
                fingerprint('c'),
                NOW.minusSeconds(10));
    }

    private static String fingerprint(char value) {
        return "sha256:" + String.valueOf(value).repeat(64);
    }
}

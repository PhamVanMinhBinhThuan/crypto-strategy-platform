package com.cryptostrategy.platform.api.experiment;

import static com.cryptostrategy.platform.api.support.AuthenticatedUsers.USER_A_ID;
import static com.cryptostrategy.platform.api.support.AuthenticatedUsers.USER_B_ID;
import static com.cryptostrategy.platform.api.support.AuthenticatedUsers.authenticatedAs;
import static com.cryptostrategy.platform.api.support.TestIdentifiers.DATASET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptostrategy.platform.domain.api.market.Asset;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.backtest.BacktestId;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktest;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktestAcceptance;
import com.cryptostrategy.platform.experiment.api.backtest.StartStandaloneBacktestCommand;
import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import com.cryptostrategy.platform.experiment.api.port.in.StartStandaloneBacktestUseCase;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:backtest-command;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=fixture-password",
            "platform.security.jwt.issuer=https://fixture.invalid/auth/v1",
            "platform.security.jwt.jwk-set-uri=https://fixture.invalid/.well-known/jwks.json",
            "platform.security.jwt.audience=authenticated"
        })
@AutoConfigureMockMvc
class IdempotencyCommandIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-09-02T10:00:00Z");
    private static final String REQUEST = """
            {
              "datasetId": "%s",
              "strategy": {
                "strategyId": "ma-crossover",
                "version": "1.0.0",
                "parameters": {"fastPeriod": 20, "slowPeriod": 50}
              },
              "configuration": {
                "initialCapital": "10000.00",
                "feeRate": "0.001",
                "slippageRate": "0",
                "positionMode": "LONG_ONLY",
                "executionPriceRule": "NEXT_CANDLE_OPEN",
                "forceCloseAtEnd": true,
                "roundingMode": "HALF_EVEN"
              }
            }
            """.formatted(DATASET_ID);

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GetDatasetUseCase datasets;
    @MockitoBean private StartStandaloneBacktestUseCase backtests;

    private final Map<String, Receipt> receipts = new HashMap<>();
    private final AtomicInteger logicalOutcomes = new AtomicInteger();

    @BeforeEach
    void configurePublishedPorts() {
        receipts.clear();
        logicalOutcomes.set(0);
        when(datasets.getDataset(new DatasetVersionId(DATASET_ID))).thenReturn(dataset());
        when(backtests.startStandaloneBacktest(
                        any(UUID.class), any(StartStandaloneBacktestCommand.class)))
                .thenAnswer(invocation -> accept(
                        invocation.getArgument(0), invocation.getArgument(1)));
    }

    @Test
    void acceptsAndReplaysTheOriginalBacktestOutcomeOneHundredTimes() throws Exception {
        String firstBody = null;
        String firstLocation = null;
        for (int attempt = 0; attempt < 100; attempt++) {
            var result = mockMvc.perform(post("/api/v1/backtests")
                            .with(authenticatedAs(USER_A_ID))
                            .header("Idempotency-Key", "backtest-100-replays")
                            .header("X-Correlation-Id", "F009-BACKTEST-REPLAY")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REQUEST))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.status").value("QUEUED"))
                    .andReturn();
            String body = result.getResponse().getContentAsString();
            String location = result.getResponse().getHeader("Location");
            if (attempt == 0) {
                firstBody = body;
                firstLocation = location;
            } else {
                assertThat(body).isEqualTo(firstBody);
                assertThat(location).isEqualTo(firstLocation);
            }
        }

        assertThat(logicalOutcomes).hasValue(1);
        assertThat(receipts).hasSize(1);
    }

    @Test
    void rejectsChangedPayloadUnderTheSameOwnerAndKey() throws Exception {
        mockMvc.perform(post("/api/v1/backtests")
                        .with(authenticatedAs(USER_A_ID))
                        .header("Idempotency-Key", "backtest-conflict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/backtests")
                        .with(authenticatedAs(USER_A_ID))
                        .header("Idempotency-Key", "backtest-conflict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST.replace("10000.00", "20000.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));

        assertThat(logicalOutcomes).hasValue(1);
    }

    @Test
    void scopesTheSameKeyByAuthenticatedOwner() throws Exception {
        mockMvc.perform(post("/api/v1/backtests")
                        .with(authenticatedAs(USER_A_ID))
                        .header("Idempotency-Key", "owner-scoped-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/backtests")
                        .with(authenticatedAs(USER_B_ID))
                        .header("Idempotency-Key", "owner-scoped-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST))
                .andExpect(status().isAccepted());

        assertThat(logicalOutcomes).hasValue(2);
        assertThat(receipts).hasSize(2);
    }

    @Test
    void returnsStableLocationAndFreezesPublishedCapabilityInputs() throws Exception {
        var result = mockMvc.perform(post("/api/v1/backtests")
                        .with(authenticatedAs(USER_A_ID))
                        .header("Idempotency-Key", "freeze-inputs")
                        .header("X-Correlation-Id", "F009-BACKTEST-FREEZE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.backtestId").isString())
                .andExpect(jsonPath("$.jobId").isString())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andReturn();

        Receipt receipt = receipts.values().iterator().next();
        StartStandaloneBacktestCommand command = receipt.command;
        assertThat(command.datasetProvenance().datasetVersionId().value()).isEqualTo(DATASET_ID);
        assertThat(command.datasetProvenance().checksum()).isEqualTo("sha256:" + "a".repeat(64));
        assertThat(command.strategyProvenance().singleStrategy().orElseThrow().pluginId().value())
                .isEqualTo("ma-crossover");
        assertThat(command.strategyProvenance().parameters().require("fastPeriod").canonicalText())
                .isEqualTo("20");
        assertThat(command.backtestConfig()).containsAllEntriesOf(Map.of(
                "initialCapital", "10000",
                "feeRate", "0.001",
                "slippageRate", "0",
                "executionPriceRule", "NEXT_CANDLE_OPEN",
                "positionMode", "LONG_ONLY",
                "forceCloseAtEnd", true,
                "roundingMode", "HALF_EVEN"));
        assertThat(result.getResponse().getHeader("Location"))
                .isEqualTo("/api/v1/jobs/" + receipt.acceptance.jobId().value());
        assertThat(receipt.acceptance.backtest().backtestId().value())
                .isNotEqualTo(receipt.acceptance.backtest().candidateId().value());
    }

    @Test
    void rejectsMissingKeyAndUnsupportedExecutionRule() throws Exception {
        mockMvc.perform(post("/api/v1/backtests")
                        .with(authenticatedAs(USER_A_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/backtests")
                        .with(authenticatedAs(USER_A_ID))
                        .header("Idempotency-Key", "invalid-rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST.replace("NEXT_CANDLE_OPEN", "CANDLE_CLOSE")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BACKTEST_CONFIGURATION_INVALID"));

        assertThat(logicalOutcomes).hasValue(0);
    }

    private StandaloneBacktestAcceptance accept(
            UUID ownerUserId, StartStandaloneBacktestCommand command) {
        String key = ownerUserId + ":" + command.idempotencyKey();
        Receipt existing = receipts.get(key);
        if (existing != null) {
            if (!existing.command.canonicalRequestHash()
                    .equals(command.canonicalRequestHash())) {
                throw new IdempotencyConflictException("conflicting payload");
            }
            var original = existing.acceptance;
            return new StandaloneBacktestAcceptance(
                    original.backtest(), original.jobId(), original.acceptedStatus(), true);
        }

        JobId jobId = JobId.generate();
        StandaloneBacktest resource = new StandaloneBacktest(
                BacktestId.generate(),
                ExperimentId.generate(),
                CandidateId.generate(),
                jobId,
                NOW);
        var acceptance = new StandaloneBacktestAcceptance(
                resource, jobId, JobStatus.QUEUED, false);
        receipts.put(key, new Receipt(command, acceptance));
        logicalOutcomes.incrementAndGet();
        return acceptance;
    }

    private static DatasetSnapshot dataset() {
        Asset btc = new Asset(
                new AssetId("01J00000000000000000000011"),
                new AssetSymbol("BTC"),
                Optional.of("Bitcoin"),
                true);
        Asset usdt = new Asset(
                new AssetId("01J00000000000000000000012"),
                new AssetSymbol("USDT"),
                Optional.of("Tether"),
                true);
        TradingPair pair = new TradingPair(
                new TradingPairId("01J00000000000000000000013"),
                btc,
                usdt,
                true);
        return new DatasetSnapshot(
                new DatasetVersionId(DATASET_ID),
                "candle-v1",
                MarketProvider.BINANCE,
                pair,
                Timeframe.FIVE_MINUTES,
                "binance-v1",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z"),
                8_928,
                "sha256:" + "a".repeat(64),
                NOW);
    }

    private record Receipt(
            StartStandaloneBacktestCommand command,
            StandaloneBacktestAcceptance acceptance) {}
}

package com.cryptostrategy.platform.api.market;

import static com.cryptostrategy.platform.api.support.AuthenticatedUsers.USER_A_ID;
import static com.cryptostrategy.platform.api.support.AuthenticatedUsers.authenticatedAs;
import static com.cryptostrategy.platform.api.support.TestIdentifiers.DATASET_ID;
import static com.cryptostrategy.platform.api.support.TestIdentifiers.opaqueId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptostrategy.platform.domain.api.market.Asset;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.CandleKey;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.model.CreateDatasetCommand;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;
import com.cryptostrategy.platform.marketdata.api.port.in.CreateDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.LoadHistoricalCandlesUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.ResolveTradingPairUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:market-api;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=fixture-password",
            "platform.security.jwt.issuer=https://fixture.invalid/auth/v1",
            "platform.security.jwt.jwk-set-uri=https://fixture.invalid/.well-known/jwks.json",
            "platform.security.jwt.audience=authenticated",
            "platform.market-data.provider=binance"
        })
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class MarketApiContractTest {
    private static final Instant START = Instant.parse("2026-09-02T00:00:00Z");
    private static final Instant END = Instant.parse("2026-09-02T00:20:00Z");
    private static final TradingPair PAIR = tradingPair();
    private static final DatasetSnapshot DATASET = dataset();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private ResolveTradingPairUseCase pairs;

    @MockitoBean
    private LoadHistoricalCandlesUseCase historical;

    @MockitoBean
    private CreateDatasetUseCase createDataset;

    @MockitoBean
    private GetDatasetUseCase getDataset;

    @BeforeEach
    void publishMarketFixtures() {
        when(pairs.resolveTradingPair(new AssetSymbol("BTC"), new AssetSymbol("USDT")))
                .thenReturn(PAIR);
        when(historical.loadHistoricalCandles(any(HistoricalCandleQuery.class)))
                .thenAnswer(invocation -> candlesFor(invocation.getArgument(0)));
        when(createDataset.createDataset(any(CreateDatasetCommand.class))).thenReturn(DATASET);
        when(getDataset.getDataset(new DatasetVersionId(DATASET_ID))).thenReturn(DATASET);
    }

    @Test
    void pagesCanonicalCandlesWithoutDuplicatesAndPreservesExactValues() throws Exception {
        String firstBody = mockMvc.perform(candleRequest(null)
                        .with(authenticatedAs(USER_A_ID))
                        .header("X-Correlation-Id", "F009-MARKET-PAGE-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "F009-MARKET-PAGE-1"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].pair").value("BTC/USDT"))
                .andExpect(jsonPath("$.items[0].timeframe").value("5m"))
                .andExpect(jsonPath("$.items[0].openTime").value("2026-09-02T00:00:00Z"))
                .andExpect(jsonPath("$.items[0].open").value("59000.100000000001"))
                .andExpect(jsonPath("$.items[0].volume").value("12.340000000001"))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode firstPage = json.readTree(firstBody);
        String cursor = firstPage.path("nextCursor").asText();
        assertThat(cursor).isNotBlank().doesNotContain("2026-09-02");

        mockMvc.perform(get("/api/v1/candles")
                        .with(authenticatedAs(USER_A_ID))
                        .queryParam("pair", "BTC/USDT")
                        .queryParam("timeframe", "5m")
                        .queryParam("startTime", START.toString())
                        .queryParam("endTime", "2026-09-02T00:25:00Z")
                        .queryParam("limit", "2")
                        .queryParam("cursor", cursor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));

        String secondBody = mockMvc.perform(candleRequest(cursor)
                        .with(authenticatedAs(USER_A_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> openTimes = new ArrayList<>();
        firstPage.path("items").forEach(item -> openTimes.add(item.path("openTime").asText()));
        json.readTree(secondBody).path("items")
                .forEach(item -> openTimes.add(item.path("openTime").asText()));
        assertThat(openTimes).containsExactly(
                "2026-09-02T00:00:00Z",
                "2026-09-02T00:05:00Z",
                "2026-09-02T00:10:00Z",
                "2026-09-02T00:15:00Z");

        ArgumentCaptor<HistoricalCandleQuery> queries =
                ArgumentCaptor.forClass(HistoricalCandleQuery.class);
        verify(historical, org.mockito.Mockito.times(2))
                .loadHistoricalCandles(queries.capture());
        assertThat(queries.getAllValues())
                .extracting(HistoricalCandleQuery::startTime, HistoricalCandleQuery::endTime)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(START, START.plus(Duration.ofMinutes(15))),
                        org.assertj.core.groups.Tuple.tuple(START.plus(Duration.ofMinutes(10)), END));
    }

    @Test
    void createsAndReadsImmutableDatasetSnapshot() throws Exception {
        String request = """
                {
                  "pair": "BTC/USDT",
                  "timeframe": "5m",
                  "startTime": "2026-09-02T00:00:00Z",
                  "endTime": "2026-09-02T00:20:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/datasets")
                        .with(authenticatedAs(USER_A_ID))
                        .header("Idempotency-Key", "dataset-request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/datasets/" + DATASET_ID))
                .andExpect(jsonPath("$.datasetId").value(DATASET_ID))
                .andExpect(jsonPath("$.version").value("candle-v1"))
                .andExpect(jsonPath("$.provider").value("BINANCE"))
                .andExpect(jsonPath("$.pair").value("BTC/USDT"))
                .andExpect(jsonPath("$.startTime").value("2026-09-02T00:00:00Z"))
                .andExpect(jsonPath("$.endTime").value("2026-09-02T00:20:00Z"))
                .andExpect(jsonPath("$.membershipCount").value(4))
                .andExpect(jsonPath("$.status").value("READY"));

        mockMvc.perform(get("/api/v1/datasets/{datasetId}", DATASET_ID)
                        .with(authenticatedAs(USER_A_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasetId").value(DATASET_ID))
                .andExpect(jsonPath("$.checksum").value(DATASET.checksum()));

        ArgumentCaptor<CreateDatasetCommand> command =
                ArgumentCaptor.forClass(CreateDatasetCommand.class);
        verify(createDataset).createDataset(command.capture());
        assertThat(command.getValue().normalizationVersion()).isEqualTo("binance-v1");
        assertThat(command.getValue().version()).isEqualTo("candle-v1");
        assertThat(command.getValue().query().startTime()).isEqualTo(START);
        assertThat(command.getValue().query().endTime()).isEqualTo(END);
    }

    @Test
    void rejectsInvalidRangeTimeframeCursorAndMissingIdempotencyKey() throws Exception {
        mockMvc.perform(get("/api/v1/candles")
                        .with(authenticatedAs(USER_A_ID))
                        .queryParam("pair", "BTC/USDT")
                        .queryParam("timeframe", "7m")
                        .queryParam("startTime", START.toString())
                        .queryParam("endTime", END.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_MARKET_QUERY"));

        mockMvc.perform(candleRequest("not/a/cursor")
                        .with(authenticatedAs(USER_A_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));

        mockMvc.perform(post("/api/v1/datasets")
                        .with(authenticatedAs(USER_A_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pair":"BTC/USDT","timeframe":"5m",
                                 "startTime":"2026-09-02T00:20:00Z",
                                 "endTime":"2026-09-02T00:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/datasets")
                        .with(authenticatedAs(USER_A_ID))
                        .header("Idempotency-Key", "invalid-range-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pair":"BTC/USDT","timeframe":"5m",
                                 "startTime":"2026-09-02T00:20:00Z",
                                 "endTime":"2026-09-02T00:00:00Z"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_MARKET_QUERY"));
    }

    @Test
    void providerFailureUsesSafeStableError(CapturedOutput output) throws Exception {
        when(historical.loadHistoricalCandles(any(HistoricalCandleQuery.class)))
                .thenThrow(new MarketDataException(
                        MarketDataErrorCode.MARKET_PROVIDER_UNAVAILABLE,
                        "provider token=raw-secret SQL select * from market.candle",
                        Map.of("rawPayload", "private-provider-body")));

        String body = mockMvc.perform(candleRequest(null)
                        .with(authenticatedAs(USER_A_ID)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.code").value("MARKET_PROVIDER_UNAVAILABLE"))
                .andExpect(jsonPath("$.details.retryable").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("raw-secret", "select *", "private-provider-body");
        assertThat(output.getAll())
                .doesNotContain("raw-secret", "select *", "private-provider-body");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder candleRequest(
            String cursor) {
        var request = get("/api/v1/candles")
                .queryParam("pair", "BTC/USDT")
                .queryParam("timeframe", "5m")
                .queryParam("startTime", START.toString())
                .queryParam("endTime", END.toString())
                .queryParam("limit", "2");
        return cursor == null ? request : request.queryParam("cursor", cursor);
    }

    private static HistoricalCandleBatch candlesFor(HistoricalCandleQuery query) {
        List<Candle> candles = new ArrayList<>();
        Instant openTime = query.startTime();
        int index = 0;
        while (openTime.isBefore(query.endTime())) {
            BigDecimal open = new BigDecimal("59000.100000000001")
                    .add(BigDecimal.valueOf(index));
            candles.add(new Candle(
                    new CandleKey(
                            query.provider(), query.tradingPair(), query.timeframe(), openTime),
                    query.timeframe().next(openTime),
                    open,
                    open.add(BigDecimal.TEN),
                    open.subtract(BigDecimal.TEN),
                    open.add(BigDecimal.ONE),
                    new BigDecimal("12.340000000001"),
                    true));
            openTime = query.timeframe().next(openTime);
            index++;
        }
        return new HistoricalCandleBatch(candles);
    }

    private static TradingPair tradingPair() {
        Asset btc = new Asset(
                new AssetId(opaqueId(101)),
                new AssetSymbol("BTC"),
                Optional.of("Bitcoin"),
                true);
        Asset usdt = new Asset(
                new AssetId(opaqueId(102)),
                new AssetSymbol("USDT"),
                Optional.of("Tether"),
                true);
        return new TradingPair(new TradingPairId(opaqueId(103)), btc, usdt, true);
    }

    private static DatasetSnapshot dataset() {
        return new DatasetSnapshot(
                new DatasetVersionId(DATASET_ID),
                "candle-v1",
                MarketProvider.BINANCE,
                PAIR,
                Timeframe.FIVE_MINUTES,
                "binance-v1",
                START,
                END,
                4,
                "sha256:" + "a".repeat(64),
                Instant.parse("2026-09-02T00:21:00Z"));
    }
}

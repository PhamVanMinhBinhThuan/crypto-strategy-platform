package com.cryptostrategy.platform.strategies.internal.sentiment;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.domain.api.market.Asset;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.StrategyContext;
import com.cryptostrategy.platform.strategy.api.model.StrategyEvidenceValue;
import com.cryptostrategy.platform.strategy.api.model.StrategyObservation;
import com.cryptostrategy.platform.strategy.api.model.StrategyObservationId;
import com.cryptostrategy.platform.strategy.api.model.StrategyInputSnapshotId;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import com.cryptostrategy.platform.strategy.api.model.StrategySupplementalInput;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SentimentPolarityStrategyTest {
    private static final Instant EVALUATION_TIME = Instant.parse("2026-09-09T12:00:00Z");

    @Test
    void emitsBuySellAndBetweenThresholdHold() {
        Strategy strategy = strategy(1);

        assertEquals(StrategySignal.BUY, strategy.evaluate(context(List.of(
                observation("buy", "2026-09-09T11:00:00Z", "0.6", "0.8")))).signal());
        assertEquals(StrategySignal.SELL, strategy.evaluate(context(List.of(
                observation("sell", "2026-09-09T11:00:00Z", "-0.6", "0.8")))).signal());
        assertEquals(StrategySignal.HOLD, strategy.evaluate(context(List.of(
                observation("hold", "2026-09-09T11:00:00Z", "0.1", "0.8")))).signal());
    }

    @Test
    void usesConfidenceWeightedPolarityAtThresholdBoundary() {
        var decision = strategy(2).evaluate(context(List.of(
                observation("positive", "2026-09-09T10:00:00Z", "1", "0.5"),
                observation("neutral", "2026-09-09T11:00:00Z", "0", "0.5"))));

        assertEquals(StrategySignal.BUY, decision.signal());
        assertEquals(new BigDecimal("0.5"),
                ((StrategyEvidenceValue.DecimalEvidence)
                        decision.evidence().get("sentimentScore")).value());
    }

    @Test
    void excludesFutureAndExpiredNewsWithoutLookAhead() {
        var decision = strategy(1).evaluate(context(List.of(
                observation("expired", "2026-09-07T11:59:59Z", "1", "1"),
                observation("eligible", "2026-09-09T11:00:00Z", "-0.7", "1"),
                observation("future", "2026-09-09T12:00:01Z", "1", "1"))));

        assertEquals(StrategySignal.SELL, decision.signal());
        assertEquals(new StrategyEvidenceValue.IntegerEvidence(1),
                decision.evidence().get("eligibleArticleCount"));
    }

    @Test
    void returnsExplainedHoldForInsufficientOrZeroWeightData() {
        var insufficient = strategy(2).evaluate(context(List.of(
                observation("one", "2026-09-09T11:00:00Z", "0.9", "1"))));
        var zeroWeight = strategy(1).evaluate(context(List.of(
                observation("zero", "2026-09-09T11:00:00Z", "0.9", "0"))));

        assertEquals("INSUFFICIENT_SENTIMENT_ARTICLES", insufficient.reasonCode());
        assertEquals("ZERO_SENTIMENT_WEIGHT", zeroWeight.reasonCode());
    }

    @Test
    void producesStableEvidenceForCanonicalInput() {
        var first = strategy(2).evaluate(context(List.of(
                observation("b", "2026-09-09T11:00:00Z", "0.4", "0.7"),
                observation("a", "2026-09-09T10:00:00Z", "0.2", "0.3"))));
        var second = strategy(2).evaluate(context(List.of(
                observation("a", "2026-09-09T10:00:00Z", "0.2", "0.3"),
                observation("b", "2026-09-09T11:00:00Z", "0.4", "0.7"))));

        assertEquals(first, second);
    }

    private static Strategy strategy(int minimumArticles) {
        return new SentimentPolarityPlugin().create(SentimentPolarityPluginTest.parameters(
                24, minimumArticles, "0.25", "-0.25"));
    }

    private static StrategyContext context(List<StrategyObservation> observations) {
        Asset btc = new Asset(new AssetId("01J00000000000000000000001"),
                new AssetSymbol("BTC"), Optional.empty(), true);
        Asset usdt = new Asset(new AssetId("01J00000000000000000000002"),
                new AssetSymbol("USDT"), Optional.empty(), true);
        TradingPair pair = new TradingPair(new TradingPairId("01J00000000000000000000003"),
                btc, usdt, true);
        var input = new StrategySupplementalInput(SentimentPolarityPlugin.INPUT_TYPE,
                SentimentPolarityPlugin.INPUT_SCHEMA_VERSION,
                new StrategyInputSnapshotId("01J00000000000000000000050"), "sha256:snapshot",
                Map.of("modelVersion", "multichannel-english-1"), observations);
        return new StrategyContext(pair, Timeframe.ONE_HOUR, List.of(), EVALUATION_TIME,
                Map.of(input.inputType(), input));
    }

    private static StrategyObservation observation(String id, String time, String value, String weight) {
        long hash = Integer.toUnsignedLong(id.hashCode()) % 1000;
        String ulid = "01J000000000000000000" + String.format("%05d", hash);
        return new StrategyObservation(new StrategyObservationId(ulid), "result-" + id, Instant.parse(time),
                new BigDecimal(value), new BigDecimal(weight), Map.of("contentHash", "hash-" + id));
    }
}

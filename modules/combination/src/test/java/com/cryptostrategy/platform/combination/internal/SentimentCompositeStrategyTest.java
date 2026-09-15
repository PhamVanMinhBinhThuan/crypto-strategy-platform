package com.cryptostrategy.platform.combination.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cryptostrategy.platform.domain.api.market.Asset;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyContext;
import com.cryptostrategy.platform.strategy.api.model.StrategyDecision;
import com.cryptostrategy.platform.strategy.api.model.StrategyInputSnapshotId;
import com.cryptostrategy.platform.strategy.api.model.StrategyObservation;
import com.cryptostrategy.platform.strategy.api.model.StrategyObservationId;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import com.cryptostrategy.platform.strategy.api.model.StrategySupplementalInput;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SentimentCompositeStrategyTest {
    private static final Instant EVALUATION_TIME = Instant.parse("2026-09-09T12:00:00Z");
    private static final StrategyReference COMPOSITE_REFERENCE = reference(
            "01J00000000000000000000060", "composite-test");
    private static final StrategyReference SENTIMENT_REFERENCE = reference(
            "01J00000000000000000000040", "sentiment-polarity");
    private static final StrategyReference TECHNICAL_REFERENCE = reference(
            "01J00000000000000000000061", "technical-test");

    @Test
    void majority_vote_returns_hold_when_sentiment_and_technical_signals_conflict() {
        Strategy sentiment = sentimentStrategy();
        Strategy technicalSell = fixedTechnical(StrategySignal.SELL);
        var composite = new CompositeStrategy(COMPOSITE_REFERENCE, new MajorityVotePolicy(),
                StrategyParameterSet.empty(), List.of(sentiment, technicalSell));

        StrategyDecision result = composite.evaluate(contextWithPositiveSentiment());

        assertEquals(StrategySignal.HOLD, result.signal());
        assertEquals("COMPOSITE_MAJORITY_VOTE", result.reasonCode());
    }

    @Test
    void weighted_vote_allows_sentiment_to_outweigh_a_conflicting_technical_signal() {
        Strategy sentiment = sentimentStrategy();
        Strategy technicalSell = fixedTechnical(StrategySignal.SELL);
        var weights = StrategyParameterSet.of(Map.of(
                weightKey(SENTIMENT_REFERENCE), decimal("2"),
                weightKey(TECHNICAL_REFERENCE), decimal("1")));
        var composite = new CompositeStrategy(COMPOSITE_REFERENCE, new WeightedVotePolicy(),
                weights, List.of(sentiment, technicalSell));

        StrategyDecision result = composite.evaluate(contextWithPositiveSentiment());

        assertEquals(StrategySignal.BUY, result.signal());
        assertEquals("COMPOSITE_WEIGHTED_VOTE", result.reasonCode());
    }

    @Test
    void weighted_vote_returns_hold_when_conflicting_signals_have_equal_weight() {
        Strategy sentiment = sentimentStrategy();
        Strategy technicalSell = fixedTechnical(StrategySignal.SELL);
        var weights = StrategyParameterSet.of(Map.of(
                weightKey(SENTIMENT_REFERENCE), decimal("1"),
                weightKey(TECHNICAL_REFERENCE), decimal("1")));
        var composite = new CompositeStrategy(COMPOSITE_REFERENCE, new WeightedVotePolicy(),
                weights, List.of(sentiment, technicalSell));

        assertEquals(StrategySignal.HOLD,
                composite.evaluate(contextWithPositiveSentiment()).signal());
    }

    private static Strategy sentimentStrategy() {
        return context -> {
            var input = context.requireSupplementalInput("sentiment-polarity");
            BigDecimal score = input.observations().getFirst().value();
            StrategySignal signal = score.compareTo(new BigDecimal("0.25")) >= 0
                    ? StrategySignal.BUY : StrategySignal.HOLD;
            return new StrategyDecision(signal, context.evaluationTime(), SENTIMENT_REFERENCE,
                    "FROZEN_SENTIMENT_FIXTURE", "Signal derived from frozen sentiment", Map.of());
        };
    }

    private static Strategy fixedTechnical(StrategySignal signal) {
        return context -> new StrategyDecision(signal, context.evaluationTime(),
                TECHNICAL_REFERENCE, "TECHNICAL_FIXTURE", "Technical test signal", Map.of());
    }

    private static StrategyContext contextWithPositiveSentiment() {
        var btc = new Asset(new AssetId("01J00000000000000000000001"),
                new AssetSymbol("BTC"), Optional.empty(), true);
        var usdt = new Asset(new AssetId("01J00000000000000000000002"),
                new AssetSymbol("USDT"), Optional.empty(), true);
        var pair = new TradingPair(new TradingPairId("01J00000000000000000000003"),
                btc, usdt, true);
        var observation = new StrategyObservation(
                new StrategyObservationId("01J00000000000000000000070"),
                "sentiment-result-positive", EVALUATION_TIME.minusSeconds(3600),
                new BigDecimal("0.8"), new BigDecimal("0.9"), Map.of());
        var snapshot = new StrategySupplementalInput(
                "sentiment-polarity", "sentiment-snapshot-v1",
                new StrategyInputSnapshotId("01J00000000000000000000071"),
                "sha256:sentiment-composite-test",
                Map.of("modelVersion", "multichannel-english-1"), List.of(observation));
        return new StrategyContext(pair, Timeframe.ONE_HOUR, List.of(), EVALUATION_TIME,
                Map.of(snapshot.inputType(), snapshot));
    }

    private static StrategyReference reference(String versionId, String pluginId) {
        return new StrategyReference(new StrategyVersionId(versionId),
                new StrategyPluginId(pluginId), new SemanticVersion(1, 0, 0));
    }

    private static String weightKey(StrategyReference reference) {
        return "weight." + reference.strategyVersionId().value();
    }

    private static StrategyParameterValue.DecimalValue decimal(String value) {
        return new StrategyParameterValue.DecimalValue(new BigDecimal(value));
    }
}

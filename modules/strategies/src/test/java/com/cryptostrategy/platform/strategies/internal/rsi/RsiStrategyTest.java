package com.cryptostrategy.platform.strategies.internal.rsi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cryptostrategy.platform.domain.api.market.Asset;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.CandleKey;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.StrategyContext;
import com.cryptostrategy.platform.strategy.api.model.StrategyDecision;
import com.cryptostrategy.platform.strategy.api.model.StrategyEvidenceValue;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.internal.parameter.StrategyParameterValidator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RsiStrategyTest {
    private static final int DEFAULT_PERIOD = 14;

    @Test
    void descriptorPublishesStableContractAndDefaults() {
        RsiPlugin plugin = new RsiPlugin();

        assertEquals("rsi-threshold", plugin.descriptor().reference().pluginId().value());
        assertEquals("1.0.0", plugin.descriptor().reference().implementationVersion().toString());
        assertEquals("strategy-contract-v1", plugin.descriptor().contractVersion());
        assertEquals(DEFAULT_PERIOD + 1, plugin.descriptor().requiredLookback());
        assertEquals(
                java.util.Set.of(StrategySignal.BUY, StrategySignal.SELL, StrategySignal.HOLD),
                plugin.descriptor().supportedSignals());

        StrategyParameterSet defaults = defaults(plugin);
        assertEquals(DEFAULT_PERIOD + 1, plugin.requiredLookback(defaults));
        assertEquals("14", defaults.require("period").canonicalText());
        assertEquals("30", defaults.require("buyThreshold").canonicalText());
        assertEquals("70", defaults.require("sellThreshold").canonicalText());
    }

    @Test
    void emitsBuySellAndHoldFromCanonicalRsiThresholds() {
        Strategy strategy = strategy();

        assertDecision(strategy.evaluate(context(descendingPrices())), StrategySignal.BUY, "0");
        assertDecision(strategy.evaluate(context(ascendingPrices())), StrategySignal.SELL, "100");
        assertDecision(strategy.evaluate(context(alternatingPrices())), StrategySignal.HOLD, "50");
    }

    @Test
    void returnsExactlyTheSameDecisionForFrozenInput() {
        Strategy strategy = strategy();
        StrategyContext frozen = context(alternatingPrices());
        StrategyDecision expected = strategy.evaluate(frozen);

        for (int evaluation = 0; evaluation < 100; evaluation++) {
            assertEquals(expected, strategy.evaluate(frozen));
        }
    }

    @Test
    void insufficientDataIsAnExplicitErrorInsteadOfHold() {
        Strategy strategy = strategy();
        StrategyException error = assertThrows(
                StrategyException.class,
                () -> strategy.evaluate(context(ascendingPrices().subList(0, DEFAULT_PERIOD))));

        assertEquals(StrategyErrorCode.INSUFFICIENT_DATA, error.code());
    }

    private static Strategy strategy() {
        RsiPlugin plugin = new RsiPlugin();
        return plugin.create(defaults(plugin));
    }

    private static StrategyParameterSet defaults(RsiPlugin plugin) {
        return new StrategyParameterValidator().resolve(plugin.descriptor().parameterSchema(), Map.of());
    }

    private static void assertDecision(
            StrategyDecision decision,
            StrategySignal expectedSignal,
            String expectedRsi) {
        assertEquals(expectedSignal, decision.signal());
        assertEquals("RSI_THRESHOLD", decision.reasonCode());
        StrategyEvidenceValue.DecimalEvidence evidence = assertInstanceOf(
                StrategyEvidenceValue.DecimalEvidence.class,
                decision.evidence().get("rsi"));
        assertEquals(0, evidence.value().compareTo(new BigDecimal(expectedRsi)));
    }

    private static List<BigDecimal> ascendingPrices() {
        List<BigDecimal> prices = new ArrayList<>();
        for (int index = 0; index <= DEFAULT_PERIOD; index++) {
            prices.add(BigDecimal.valueOf(100L + index));
        }
        return prices;
    }

    private static List<BigDecimal> descendingPrices() {
        List<BigDecimal> prices = new ArrayList<>();
        for (int index = 0; index <= DEFAULT_PERIOD; index++) {
            prices.add(BigDecimal.valueOf(100L - index));
        }
        return prices;
    }

    private static List<BigDecimal> alternatingPrices() {
        List<BigDecimal> prices = new ArrayList<>();
        prices.add(BigDecimal.valueOf(100));
        for (int index = 1; index <= DEFAULT_PERIOD; index++) {
            long delta = index % 2 == 1 ? 1 : 0;
            prices.add(BigDecimal.valueOf(100 + delta));
        }
        return prices;
    }

    private static StrategyContext context(List<BigDecimal> prices) {
        Asset btc = new Asset(
                new AssetId("01J00000000000000000000001"),
                new AssetSymbol("BTC"),
                Optional.empty(),
                true);
        Asset usdt = new Asset(
                new AssetId("01J00000000000000000000002"),
                new AssetSymbol("USDT"),
                Optional.empty(),
                true);
        TradingPair pair = new TradingPair(
                new TradingPairId("01J00000000000000000000003"), btc, usdt, true);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        List<Candle> candles = new ArrayList<>();
        for (int index = 0; index < prices.size(); index++) {
            Instant open = start.plusSeconds(60L * index);
            BigDecimal price = prices.get(index);
            candles.add(new Candle(
                    new CandleKey(MarketProvider.BINANCE, pair, Timeframe.ONE_MINUTE, open),
                    open.plusSeconds(60),
                    price,
                    price,
                    price,
                    price,
                    BigDecimal.ONE,
                    true));
        }
        return new StrategyContext(
                pair, Timeframe.ONE_MINUTE, candles, candles.getLast().closeTime());
    }
}

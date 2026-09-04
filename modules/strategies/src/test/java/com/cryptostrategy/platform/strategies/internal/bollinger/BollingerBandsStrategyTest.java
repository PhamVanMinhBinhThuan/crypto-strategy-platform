package com.cryptostrategy.platform.strategies.internal.bollinger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.Set;
import org.junit.jupiter.api.Test;

class BollingerBandsStrategyTest {
    private static final int DEFAULT_PERIOD = 20;

    @Test
    void descriptorPublishesStableContractAndDefaults() {
        BollingerBandsPlugin plugin = new BollingerBandsPlugin();

        assertEquals("bollinger-bands", plugin.descriptor().reference().pluginId().value());
        assertEquals("1.0.0", plugin.descriptor().reference().implementationVersion().toString());
        assertEquals("strategy-contract-v1", plugin.descriptor().contractVersion());
        assertEquals("VOLATILITY", plugin.descriptor().category());
        assertEquals(DEFAULT_PERIOD, plugin.descriptor().requiredLookback());
        assertEquals(
                Set.of(StrategySignal.BUY, StrategySignal.SELL, StrategySignal.HOLD),
                plugin.descriptor().supportedSignals());

        StrategyParameterSet defaults = defaults(plugin);
        assertEquals(DEFAULT_PERIOD, plugin.requiredLookback(defaults));
        assertEquals("20", defaults.require("period").canonicalText());
        assertEquals("2", defaults.require("standardDeviation").canonicalText());
        assertEquals("MEAN_REVERSION", defaults.require("ruleMode").canonicalText());
    }

    @Test
    void meanReversionBuysBelowLowerBandSellsAboveUpperBandAndHoldsInside() {
        Strategy strategy = strategy();

        assertBandDecision(strategy.evaluate(context(pricesWithLast(new BigDecimal("50")))), StrategySignal.BUY);
        assertBandDecision(strategy.evaluate(context(pricesWithLast(new BigDecimal("150")))), StrategySignal.SELL);
        assertBandDecision(strategy.evaluate(context(constantPrices())), StrategySignal.HOLD);
    }

    @Test
    void returnsExactlyTheSameDecisionAndEvidenceForFrozenInput() {
        Strategy strategy = strategy();
        StrategyContext frozen = context(pricesWithLast(new BigDecimal("150")));
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
                () -> strategy.evaluate(context(constantPrices().subList(0, DEFAULT_PERIOD - 1))));

        assertEquals(StrategyErrorCode.INSUFFICIENT_DATA, error.code());
    }

    private static Strategy strategy() {
        BollingerBandsPlugin plugin = new BollingerBandsPlugin();
        return plugin.create(defaults(plugin));
    }

    private static StrategyParameterSet defaults(BollingerBandsPlugin plugin) {
        return new StrategyParameterValidator().resolve(plugin.descriptor().parameterSchema(), Map.of());
    }

    private static void assertBandDecision(StrategyDecision decision, StrategySignal expectedSignal) {
        assertEquals(expectedSignal, decision.signal());
        assertEquals("BOLLINGER_BAND", decision.reasonCode());
        assertInstanceOf(
                StrategyEvidenceValue.DecimalEvidence.class,
                decision.evidence().get("middleBand"));
        StrategyEvidenceValue.DecimalEvidence lower = assertInstanceOf(
                StrategyEvidenceValue.DecimalEvidence.class,
                decision.evidence().get("lowerBand"));
        StrategyEvidenceValue.DecimalEvidence upper = assertInstanceOf(
                StrategyEvidenceValue.DecimalEvidence.class,
                decision.evidence().get("upperBand"));
        assertTrue(lower.value().compareTo(upper.value()) <= 0);
    }

    private static List<BigDecimal> constantPrices() {
        List<BigDecimal> prices = new ArrayList<>();
        for (int index = 0; index < DEFAULT_PERIOD; index++) {
            prices.add(new BigDecimal("100"));
        }
        return prices;
    }

    private static List<BigDecimal> pricesWithLast(BigDecimal last) {
        List<BigDecimal> prices = constantPrices();
        prices.set(prices.size() - 1, last);
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

package com.cryptostrategy.platform.strategies.internal.support;

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

class SupportResistanceStrategyTest {
    private static final int DEFAULT_LOOKBACK = 20;

    @Test
    void descriptorPublishesStableContractAndDefaults() {
        SupportResistancePlugin plugin = new SupportResistancePlugin();

        assertEquals("support-resistance", plugin.descriptor().reference().pluginId().value());
        assertEquals("1.0.0", plugin.descriptor().reference().implementationVersion().toString());
        assertEquals("strategy-contract-v1", plugin.descriptor().contractVersion());
        assertEquals("STRUCTURE", plugin.descriptor().category());
        assertEquals(DEFAULT_LOOKBACK + 1, plugin.descriptor().requiredLookback());
        assertEquals(
                Set.of(StrategySignal.BUY, StrategySignal.SELL, StrategySignal.HOLD),
                plugin.descriptor().supportedSignals());

        StrategyParameterSet defaults = defaults(plugin);
        assertEquals(DEFAULT_LOOKBACK + 1, plugin.requiredLookback(defaults));
        assertEquals("20", defaults.require("lookback").canonicalText());
        assertEquals("1", defaults.require("tolerancePercent").canonicalText());
        assertEquals("BOUNCE", defaults.require("ruleMode").canonicalText());
    }

    @Test
    void bounceModeBuysNearSupportSellsNearResistanceAndHoldsBetweenZones() {
        Strategy strategy = strategy();

        assertZoneDecision(strategy.evaluate(context(pricesWithCurrent("90"))), StrategySignal.BUY);
        assertZoneDecision(strategy.evaluate(context(pricesWithCurrent("110"))), StrategySignal.SELL);
        assertZoneDecision(strategy.evaluate(context(pricesWithCurrent("100"))), StrategySignal.HOLD);
    }

    @Test
    void currentCandleDoesNotChangeTheHistoricalSupportAndResistanceLevels() {
        Strategy strategy = strategy();

        StrategyDecision nearSupport = strategy.evaluate(context(pricesWithCurrent("90")));
        StrategyDecision nearResistance = strategy.evaluate(context(pricesWithCurrent("110")));

        assertEquals(nearSupport.evidence().get("supportLevel"), nearResistance.evidence().get("supportLevel"));
        assertEquals(
                nearSupport.evidence().get("resistanceLevel"),
                nearResistance.evidence().get("resistanceLevel"));
    }

    @Test
    void returnsExactlyTheSameDecisionAndEvidenceForFrozenInput() {
        Strategy strategy = strategy();
        StrategyContext frozen = context(pricesWithCurrent("90"));
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
                () -> strategy.evaluate(context(historicalPrices())));

        assertEquals(StrategyErrorCode.INSUFFICIENT_DATA, error.code());
    }

    private static Strategy strategy() {
        SupportResistancePlugin plugin = new SupportResistancePlugin();
        return plugin.create(defaults(plugin));
    }

    private static StrategyParameterSet defaults(SupportResistancePlugin plugin) {
        return new StrategyParameterValidator().resolve(plugin.descriptor().parameterSchema(), Map.of());
    }

    private static void assertZoneDecision(StrategyDecision decision, StrategySignal expectedSignal) {
        assertEquals(expectedSignal, decision.signal());
        assertEquals("SUPPORT_RESISTANCE_BOUNCE", decision.reasonCode());
        StrategyEvidenceValue.DecimalEvidence support = assertInstanceOf(
                StrategyEvidenceValue.DecimalEvidence.class,
                decision.evidence().get("supportLevel"));
        StrategyEvidenceValue.DecimalEvidence resistance = assertInstanceOf(
                StrategyEvidenceValue.DecimalEvidence.class,
                decision.evidence().get("resistanceLevel"));
        assertInstanceOf(
                StrategyEvidenceValue.DecimalEvidence.class,
                decision.evidence().get("distancePercent"));
        assertEquals(0, support.value().compareTo(new BigDecimal("90")));
        assertEquals(0, resistance.value().compareTo(new BigDecimal("110")));
        assertTrue(support.value().compareTo(resistance.value()) < 0);
    }

    private static List<BigDecimal> historicalPrices() {
        List<BigDecimal> prices = new ArrayList<>();
        for (int index = 0; index < DEFAULT_LOOKBACK; index++) {
            prices.add(index % 2 == 0 ? new BigDecimal("90") : new BigDecimal("110"));
        }
        return prices;
    }

    private static List<BigDecimal> pricesWithCurrent(String current) {
        List<BigDecimal> prices = historicalPrices();
        prices.add(new BigDecimal(current));
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

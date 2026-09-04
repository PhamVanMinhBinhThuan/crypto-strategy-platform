package com.cryptostrategy.platform.strategies.internal.support;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.StrategyContext;
import com.cryptostrategy.platform.strategy.api.model.StrategyDecision;
import com.cryptostrategy.platform.strategy.api.model.StrategyEvidenceValue;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SupportResistanceStrategy implements Strategy {
    private static final MathContext MATH = MathContext.DECIMAL128;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final String BOUNCE = "BOUNCE";

    private final StrategyReference reference;
    private final int lookback;
    private final BigDecimal tolerancePercent;
    private final String ruleMode;

    public SupportResistanceStrategy(
            StrategyReference reference,
            int lookback,
            BigDecimal tolerancePercent,
            String ruleMode) {
        this.reference = Objects.requireNonNull(reference);
        this.tolerancePercent = Objects.requireNonNull(tolerancePercent);
        this.ruleMode = Objects.requireNonNull(ruleMode);
        if (lookback < 2) {
            throw new IllegalArgumentException("Support/resistance lookback must be at least 2");
        }
        if (tolerancePercent.signum() < 0 || tolerancePercent.compareTo(new BigDecimal("25")) > 0) {
            throw new IllegalArgumentException("Tolerance percent must be between 0 and 25");
        }
        if (!BOUNCE.equals(ruleMode)) {
            throw new IllegalArgumentException("Unsupported support/resistance rule mode: " + ruleMode);
        }
        this.lookback = lookback;
    }

    @Override
    public StrategyDecision evaluate(StrategyContext context) {
        Objects.requireNonNull(context);
        int requiredCandles = Math.addExact(lookback, 1);
        if (context.candles().size() < requiredCandles) {
            throw new StrategyException(
                    StrategyErrorCode.INSUFFICIENT_DATA,
                    "Support/resistance requires " + requiredCandles + " Candles");
        }

        List<Candle> candles = context.candles();
        List<Candle> historicalWindow = candles.subList(
                candles.size() - requiredCandles, candles.size() - 1);
        BigDecimal supportLevel = historicalWindow.stream()
                .map(Candle::close)
                .min(Comparator.naturalOrder())
                .orElseThrow();
        BigDecimal resistanceLevel = historicalWindow.stream()
                .map(Candle::close)
                .max(Comparator.naturalOrder())
                .orElseThrow();
        BigDecimal currentPrice = candles.getLast().close();

        BigDecimal supportDistance = distancePercent(currentPrice, supportLevel);
        BigDecimal resistanceDistance = distancePercent(currentPrice, resistanceLevel);
        boolean nearSupport = currentPrice.compareTo(supportLevel) <= 0
                || supportDistance.compareTo(tolerancePercent) <= 0;
        boolean nearResistance = currentPrice.compareTo(resistanceLevel) >= 0
                || resistanceDistance.compareTo(tolerancePercent) <= 0;

        StrategySignal signal;
        BigDecimal reportedDistance;
        if (nearSupport && (!nearResistance || supportDistance.compareTo(resistanceDistance) <= 0)) {
            signal = StrategySignal.BUY;
            reportedDistance = supportDistance;
        } else if (nearResistance) {
            signal = StrategySignal.SELL;
            reportedDistance = resistanceDistance;
        } else {
            signal = StrategySignal.HOLD;
            reportedDistance = supportDistance.min(resistanceDistance);
        }

        return new StrategyDecision(
                signal,
                context.evaluationTime(),
                reference,
                "SUPPORT_RESISTANCE_BOUNCE",
                "Closing price compared with historical support and resistance zones",
                Map.of(
                        "currentPrice",
                        new StrategyEvidenceValue.DecimalEvidence(currentPrice),
                        "distancePercent",
                        new StrategyEvidenceValue.DecimalEvidence(reportedDistance),
                        "resistanceLevel",
                        new StrategyEvidenceValue.DecimalEvidence(resistanceLevel),
                        "ruleMode",
                        new StrategyEvidenceValue.TextEvidence(ruleMode),
                        "supportLevel",
                        new StrategyEvidenceValue.DecimalEvidence(supportLevel)));
    }

    private static BigDecimal distancePercent(BigDecimal price, BigDecimal level) {
        if (level.signum() == 0) {
            return price.signum() == 0 ? BigDecimal.ZERO : ONE_HUNDRED;
        }
        return price.subtract(level).abs().multiply(ONE_HUNDRED).divide(level.abs(), MATH);
    }
}

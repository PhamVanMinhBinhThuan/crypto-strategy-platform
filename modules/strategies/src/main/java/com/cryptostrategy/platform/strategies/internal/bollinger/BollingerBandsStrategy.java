package com.cryptostrategy.platform.strategies.internal.bollinger;

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
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BollingerBandsStrategy implements Strategy {
    private static final MathContext MATH = MathContext.DECIMAL128;
    private static final String MEAN_REVERSION = "MEAN_REVERSION";

    private final StrategyReference reference;
    private final int period;
    private final BigDecimal standardDeviationMultiplier;
    private final String ruleMode;

    public BollingerBandsStrategy(
            StrategyReference reference,
            int period,
            BigDecimal standardDeviationMultiplier,
            String ruleMode) {
        this.reference = Objects.requireNonNull(reference);
        this.standardDeviationMultiplier = Objects.requireNonNull(standardDeviationMultiplier);
        this.ruleMode = Objects.requireNonNull(ruleMode);
        if (period < 2) {
            throw new IllegalArgumentException("Bollinger period must be at least 2");
        }
        if (standardDeviationMultiplier.signum() <= 0) {
            throw new IllegalArgumentException("Standard deviation multiplier must be positive");
        }
        if (!MEAN_REVERSION.equals(ruleMode)) {
            throw new IllegalArgumentException("Unsupported Bollinger rule mode: " + ruleMode);
        }
        this.period = period;
    }

    @Override
    public StrategyDecision evaluate(StrategyContext context) {
        Objects.requireNonNull(context);
        if (context.candles().size() < period) {
            throw new StrategyException(
                    StrategyErrorCode.INSUFFICIENT_DATA,
                    "Bollinger Bands requires " + period + " Candles");
        }

        List<Candle> window = context.candles().subList(
                context.candles().size() - period, context.candles().size());
        BigDecimal middleBand = window.stream()
                .map(Candle::close)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(period), MATH);
        BigDecimal variance = window.stream()
                .map(Candle::close)
                .map(close -> close.subtract(middleBand).pow(2, MATH))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(period), MATH);
        BigDecimal standardDeviation = variance.sqrt(MATH);
        BigDecimal bandWidth = standardDeviation.multiply(standardDeviationMultiplier, MATH);
        BigDecimal lowerBand = middleBand.subtract(bandWidth, MATH);
        BigDecimal upperBand = middleBand.add(bandWidth, MATH);
        BigDecimal currentPrice = window.getLast().close();

        StrategySignal signal = currentPrice.compareTo(lowerBand) < 0
                ? StrategySignal.BUY
                : currentPrice.compareTo(upperBand) > 0
                        ? StrategySignal.SELL
                        : StrategySignal.HOLD;
        return new StrategyDecision(
                signal,
                context.evaluationTime(),
                reference,
                "BOLLINGER_BAND",
                "Closing price compared with configured Bollinger Bands",
                Map.of(
                        "currentPrice",
                        new StrategyEvidenceValue.DecimalEvidence(currentPrice),
                        "lowerBand",
                        new StrategyEvidenceValue.DecimalEvidence(lowerBand),
                        "middleBand",
                        new StrategyEvidenceValue.DecimalEvidence(middleBand),
                        "ruleMode",
                        new StrategyEvidenceValue.TextEvidence(ruleMode),
                        "upperBand",
                        new StrategyEvidenceValue.DecimalEvidence(upperBand)));
    }
}

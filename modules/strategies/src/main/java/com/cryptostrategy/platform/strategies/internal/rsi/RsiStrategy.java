package com.cryptostrategy.platform.strategies.internal.rsi;

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

public final class RsiStrategy implements Strategy {
    private static final MathContext MATH = MathContext.DECIMAL128;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal NEUTRAL_RSI = new BigDecimal("50");

    private final StrategyReference reference;
    private final int period;
    private final BigDecimal buyThreshold;
    private final BigDecimal sellThreshold;

    public RsiStrategy(
            StrategyReference reference,
            int period,
            BigDecimal buyThreshold,
            BigDecimal sellThreshold) {
        this.reference = Objects.requireNonNull(reference);
        this.buyThreshold = Objects.requireNonNull(buyThreshold);
        this.sellThreshold = Objects.requireNonNull(sellThreshold);
        if (period < 2) {
            throw new IllegalArgumentException("RSI period must be at least 2");
        }
        if (buyThreshold.signum() < 0
                || sellThreshold.compareTo(ONE_HUNDRED) > 0
                || buyThreshold.compareTo(sellThreshold) >= 0) {
            throw new IllegalArgumentException(
                    "Expected 0 <= buyThreshold < sellThreshold <= 100");
        }
        this.period = period;
    }

    @Override
    public StrategyDecision evaluate(StrategyContext context) {
        Objects.requireNonNull(context);
        int requiredCandles = Math.addExact(period, 1);
        if (context.candles().size() < requiredCandles) {
            throw new StrategyException(
                    StrategyErrorCode.INSUFFICIENT_DATA,
                    "RSI requires " + requiredCandles + " Candles");
        }

        List<Candle> window = context.candles().subList(
                context.candles().size() - requiredCandles, context.candles().size());
        BigDecimal gains = BigDecimal.ZERO;
        BigDecimal losses = BigDecimal.ZERO;
        for (int index = 1; index < window.size(); index++) {
            BigDecimal change = window.get(index).close().subtract(window.get(index - 1).close());
            if (change.signum() > 0) {
                gains = gains.add(change);
            } else if (change.signum() < 0) {
                losses = losses.add(change.abs());
            }
        }

        BigDecimal rsi = calculateRsi(gains, losses);
        StrategySignal signal = rsi.compareTo(buyThreshold) <= 0
                ? StrategySignal.BUY
                : rsi.compareTo(sellThreshold) >= 0
                        ? StrategySignal.SELL
                        : StrategySignal.HOLD;
        return new StrategyDecision(
                signal,
                context.evaluationTime(),
                reference,
                "RSI_THRESHOLD",
                "RSI compared with configured buy and sell thresholds",
                Map.of(
                        "buyThreshold",
                        new StrategyEvidenceValue.DecimalEvidence(buyThreshold),
                        "rsi",
                        new StrategyEvidenceValue.DecimalEvidence(rsi),
                        "sellThreshold",
                        new StrategyEvidenceValue.DecimalEvidence(sellThreshold)));
    }

    private static BigDecimal calculateRsi(BigDecimal gains, BigDecimal losses) {
        if (gains.signum() == 0 && losses.signum() == 0) {
            return NEUTRAL_RSI;
        }
        if (losses.signum() == 0) {
            return ONE_HUNDRED;
        }
        if (gains.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal relativeStrength = gains.divide(losses, MATH);
        return ONE_HUNDRED.subtract(
                ONE_HUNDRED.divide(BigDecimal.ONE.add(relativeStrength), MATH));
    }
}

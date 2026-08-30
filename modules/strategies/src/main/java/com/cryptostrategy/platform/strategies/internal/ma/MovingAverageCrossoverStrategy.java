package com.cryptostrategy.platform.strategies.internal.ma;

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

public final class MovingAverageCrossoverStrategy implements Strategy {
    private static final MathContext MATH = MathContext.DECIMAL128;
    private final StrategyReference reference; private final int fastPeriod; private final int slowPeriod;
    public MovingAverageCrossoverStrategy(StrategyReference reference, int fastPeriod, int slowPeriod) {
        if (fastPeriod < 1 || slowPeriod <= fastPeriod) throw new IllegalArgumentException("Expected 0 < fastPeriod < slowPeriod");
        this.reference = reference; this.fastPeriod = fastPeriod; this.slowPeriod = slowPeriod;
    }
    @Override public StrategyDecision evaluate(StrategyContext context) {
        if (context.candles().size() < slowPeriod) throw new StrategyException(StrategyErrorCode.INSUFFICIENT_DATA, "Moving Average requires " + slowPeriod + " Candles");
        List<Candle> window = context.candles().subList(context.candles().size() - slowPeriod, context.candles().size());
        BigDecimal slow = average(window, slowPeriod); BigDecimal fast = average(window, fastPeriod);
        int comparison = fast.compareTo(slow); StrategySignal signal = comparison > 0 ? StrategySignal.BUY : comparison < 0 ? StrategySignal.SELL : StrategySignal.HOLD;
        return new StrategyDecision(signal, context.evaluationTime(), reference, "MA_CROSSOVER", "Fast average compared with slow average",
                Map.of("fastAverage", new StrategyEvidenceValue.DecimalEvidence(fast), "slowAverage", new StrategyEvidenceValue.DecimalEvidence(slow)));
    }
    private static BigDecimal average(List<Candle> window, int period) {
        return window.subList(window.size() - period, window.size()).stream().map(Candle::close).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(period), MATH);
    }
}

package com.cryptostrategy.platform.backtesting.api.model;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
public record BacktestAssumptions(String contractVersion, Money initialCapital, BigDecimal feeRate,
        BigDecimal slippageRate, PositionMode positionMode, ExecutionPriceRule executionPriceRule,
        boolean forceCloseAtEnd, RoundingMode roundingMode) {
    public BacktestAssumptions {
        Objects.requireNonNull(contractVersion); Objects.requireNonNull(initialCapital); Objects.requireNonNull(feeRate);
        Objects.requireNonNull(slippageRate); Objects.requireNonNull(positionMode); Objects.requireNonNull(executionPriceRule); Objects.requireNonNull(roundingMode);
        if (contractVersion.isBlank() || initialCapital.value().signum() <= 0) throw new IllegalArgumentException("Invalid assumptions");
        feeRate = rate(feeRate); slippageRate = rate(slippageRate);
    }
    public static BacktestAssumptions mvp(BigDecimal capital, BigDecimal fee, BigDecimal slippage) {
        return new BacktestAssumptions("backtest-assumptions-v1", Money.of(capital), fee, slippage,
                PositionMode.LONG_ONLY, ExecutionPriceRule.NEXT_CANDLE_OPEN, true, RoundingMode.HALF_EVEN);
    }
    private static BigDecimal rate(BigDecimal value) {
        BigDecimal normalized=value.setScale(10,RoundingMode.HALF_EVEN);
        if(normalized.signum()<0 || normalized.compareTo(BigDecimal.ONE)>=0) throw new IllegalArgumentException("Rate must be in [0,1)");
        return normalized;
    }
}

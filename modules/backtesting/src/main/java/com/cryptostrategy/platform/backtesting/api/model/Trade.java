package com.cryptostrategy.platform.backtesting.api.model;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
public record Trade(TradeId tradeId, BacktestResultId backtestResultId, int sequence, PositionSide side,
        Instant entryTime, Instant exitTime, Money entryPrice, Money exitPrice, Quantity quantity,
        Money entryFee, Money exitFee, Money totalFee, BigDecimal realizedPnl, Money postTradeCash, ExitReason exitReason) {
    public Trade { Objects.requireNonNull(tradeId);Objects.requireNonNull(backtestResultId);Objects.requireNonNull(side);Objects.requireNonNull(entryTime);Objects.requireNonNull(exitTime);Objects.requireNonNull(entryPrice);Objects.requireNonNull(exitPrice);Objects.requireNonNull(quantity);Objects.requireNonNull(entryFee);Objects.requireNonNull(exitFee);Objects.requireNonNull(totalFee);Objects.requireNonNull(realizedPnl);Objects.requireNonNull(postTradeCash);Objects.requireNonNull(exitReason);if(sequence<0||!entryTime.isBefore(exitTime))throw new IllegalArgumentException("Invalid Trade");if(totalFee.value().compareTo(entryFee.value().add(exitFee.value()).setScale(12,java.math.RoundingMode.HALF_EVEN))!=0)throw new IllegalArgumentException("Fee total mismatch");realizedPnl=realizedPnl.setScale(12,java.math.RoundingMode.HALF_EVEN); }
}

package com.cryptostrategy.platform.backtesting.internal;

import com.cryptostrategy.platform.backtesting.api.model.*;
import com.cryptostrategy.platform.domain.api.market.Candle;
import java.math.*;
import java.time.Instant;

final class TradeExecutionPolicy {
    private static final int CALCULATION_SCALE = 24;

    Position open(Candle candle, BigDecimal cash, BacktestAssumptions assumptions) {
        BigDecimal fill = candle.open().multiply(BigDecimal.ONE.add(assumptions.slippageRate()));
        BigDecimal quantity = cash.divide(fill.multiply(BigDecimal.ONE.add(assumptions.feeRate())),
                CALCULATION_SCALE, RoundingMode.HALF_EVEN);
        BigDecimal notional = quantity.multiply(fill);
        BigDecimal fee = notional.multiply(assumptions.feeRate());
        if (notional.add(fee).compareTo(cash.add(new BigDecimal("0.000000000001"))) > 0) {
            throw new IllegalStateException("Entry cost exceeds available cash");
        }
        return new Position(candle.key().openTime(), Money.of(fill), new Quantity(quantity),
                Money.of(fee), Money.of(notional));
    }

    ClosedTrade close(BacktestResultId resultId, int sequence, Position position, BigDecimal rawPrice,
            Instant time, ExitReason reason, BacktestAssumptions assumptions) {
        BigDecimal fill = rawPrice.multiply(BigDecimal.ONE.subtract(assumptions.slippageRate()));
        BigDecimal proceeds = position.quantity().value().multiply(fill);
        BigDecimal exitFee = proceeds.multiply(assumptions.feeRate());
        BigDecimal cash = proceeds.subtract(exitFee);
        BigDecimal pnl = cash.subtract(position.entryNotional().value()).subtract(position.entryFee().value());
        Money entryFee = position.entryFee(); Money canonicalExitFee = Money.of(exitFee);
        Trade trade = new Trade(TradeId.generate(), resultId, sequence, PositionSide.LONG,
                position.entryTime(), time, position.entryPrice(), Money.of(fill), position.quantity(), entryFee,
                canonicalExitFee, Money.of(entryFee.value().add(canonicalExitFee.value())), pnl, Money.of(cash), reason);
        return new ClosedTrade(trade, cash);
    }

    record ClosedTrade(Trade trade, BigDecimal cash) {}
}

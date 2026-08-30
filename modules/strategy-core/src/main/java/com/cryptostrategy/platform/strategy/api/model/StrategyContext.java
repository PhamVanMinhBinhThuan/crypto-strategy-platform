package com.cryptostrategy.platform.strategy.api.model;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record StrategyContext(TradingPair tradingPair, Timeframe timeframe, List<Candle> candles,
        Instant evaluationTime) {
    public StrategyContext {
        Objects.requireNonNull(tradingPair); Objects.requireNonNull(timeframe); Objects.requireNonNull(candles); Objects.requireNonNull(evaluationTime);
        candles = List.copyOf(candles);
        Instant previous = null;
        for (Candle candle : candles) {
            if (!candle.closed()) throw new IllegalArgumentException("Strategy requires closed Candles");
            if (!candle.key().tradingPair().tradingPairId().equals(tradingPair.tradingPairId()) || candle.key().timeframe() != timeframe) throw new IllegalArgumentException("Mixed Candle context");
            if (previous != null && !candle.key().openTime().isAfter(previous)) throw new IllegalArgumentException("Candles must be strictly ordered");
            previous = candle.key().openTime();
        }
        if (!candles.isEmpty() && !evaluationTime.equals(candles.getLast().closeTime())) throw new IllegalArgumentException("Evaluation time must match last Candle close");
    }
}

package com.cryptostrategy.platform.domain.api.market;

import java.time.Instant;
import java.util.Objects;

public record CandleKey(MarketProvider provider, TradingPair tradingPair, Timeframe timeframe, Instant openTime) {
    public CandleKey {
        Objects.requireNonNull(provider, "provider"); Objects.requireNonNull(tradingPair, "tradingPair");
        Objects.requireNonNull(timeframe, "timeframe"); Objects.requireNonNull(openTime, "openTime");
        if (!timeframe.isAligned(openTime)) throw new IllegalArgumentException("Candle open time must be aligned");
    }
}

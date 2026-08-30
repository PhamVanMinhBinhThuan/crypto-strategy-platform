package com.cryptostrategy.platform.marketdata.api.event;

import com.cryptostrategy.platform.domain.api.market.Candle;
import java.time.Instant;
import java.util.Objects;

public record CandleUpdate(Candle candle, Instant providerEventTime) {
    public CandleUpdate { Objects.requireNonNull(candle, "candle"); Objects.requireNonNull(providerEventTime, "providerEventTime"); }
}

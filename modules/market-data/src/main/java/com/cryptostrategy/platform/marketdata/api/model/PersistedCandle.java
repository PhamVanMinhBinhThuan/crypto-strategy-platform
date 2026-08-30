package com.cryptostrategy.platform.marketdata.api.model;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.CandleId;
import java.util.Objects;

public record PersistedCandle(CandleId candleId, Candle candle) {
    public PersistedCandle { Objects.requireNonNull(candleId); Objects.requireNonNull(candle); if (!candle.closed()) throw new IllegalArgumentException("Persisted Candle must be closed"); }
}

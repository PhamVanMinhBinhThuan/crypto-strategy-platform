package com.cryptostrategy.platform.marketdata.api.model;

import com.cryptostrategy.platform.domain.api.market.Candle;
import java.util.List;

public record DatasetFinalization(DatasetSnapshot snapshot, List<Candle> candles) {
    public DatasetFinalization { candles = List.copyOf(candles); if (candles.size() != snapshot.candleCount()) throw new IllegalArgumentException("Count mismatch"); }
}

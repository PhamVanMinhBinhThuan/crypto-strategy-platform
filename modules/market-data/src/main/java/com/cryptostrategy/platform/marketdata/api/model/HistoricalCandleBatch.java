package com.cryptostrategy.platform.marketdata.api.model;

import com.cryptostrategy.platform.domain.api.market.Candle;
import java.util.List;

public record HistoricalCandleBatch(List<Candle> candles) {
    public HistoricalCandleBatch { candles = List.copyOf(candles); }
}

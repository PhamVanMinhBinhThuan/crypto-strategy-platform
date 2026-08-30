package com.cryptostrategy.platform.marketdata.api.model;

import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import java.time.Instant;
import java.util.Objects;

public record HistoricalCandleQuery(MarketProvider provider, TradingPair tradingPair, Timeframe timeframe,
                                    Instant startTime, Instant endTime, Instant collectionCutoff,
                                    int pageSize, int maxPages) {
    public HistoricalCandleQuery {
        Objects.requireNonNull(provider, "provider"); Objects.requireNonNull(tradingPair, "tradingPair");
        Objects.requireNonNull(timeframe, "timeframe"); Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(endTime, "endTime"); Objects.requireNonNull(collectionCutoff, "collectionCutoff");
        if (!startTime.isBefore(endTime) || !timeframe.isAligned(startTime) || !timeframe.isAligned(endTime)
                || pageSize < 1 || pageSize > 1000 || maxPages < 1) throw new IllegalArgumentException("Invalid historical query");
    }
}

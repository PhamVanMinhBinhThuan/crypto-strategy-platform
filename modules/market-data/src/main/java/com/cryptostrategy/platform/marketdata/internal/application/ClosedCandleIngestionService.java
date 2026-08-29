package com.cryptostrategy.platform.marketdata.internal.application;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.model.PersistedCandle;
import com.cryptostrategy.platform.marketdata.api.port.out.ClosedCandleStore;
import java.util.List;

public final class ClosedCandleIngestionService {
    private final ClosedCandleStore store;
    public ClosedCandleIngestionService(ClosedCandleStore store) { this.store = store; }
    public List<PersistedCandle> accept(List<Candle> candles) {
        if (candles.stream().anyMatch(candle -> !candle.closed())) throw new MarketDataException(MarketDataErrorCode.INVALID_MARKET_QUERY, "Batch contains an open Candle");
        return store.saveClosedBatch(candles);
    }
}

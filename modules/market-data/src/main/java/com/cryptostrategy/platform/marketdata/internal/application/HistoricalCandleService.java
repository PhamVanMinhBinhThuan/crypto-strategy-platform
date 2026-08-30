package com.cryptostrategy.platform.marketdata.internal.application;

import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;
import com.cryptostrategy.platform.marketdata.api.port.in.LoadHistoricalCandlesUseCase;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketDataProvider;
import com.cryptostrategy.platform.marketdata.internal.validation.CandleSetValidator;
import java.util.Objects;

public final class HistoricalCandleService implements LoadHistoricalCandlesUseCase {
    private final MarketDataProvider provider;
    public HistoricalCandleService(MarketDataProvider provider) { this.provider = Objects.requireNonNull(provider); }
    @Override public HistoricalCandleBatch loadHistoricalCandles(HistoricalCandleQuery query) {
        if (!provider.providerId().equals(query.provider())) throw new IllegalArgumentException("Provider mismatch");
        return new HistoricalCandleBatch(CandleSetValidator.normalizeComplete(query, provider.loadHistorical(query).candles()));
    }
}

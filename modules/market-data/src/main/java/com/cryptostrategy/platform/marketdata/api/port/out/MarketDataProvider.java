package com.cryptostrategy.platform.marketdata.api.port.out;

import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.marketdata.api.event.CandleUpdateHandler;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionStateHandler;
import com.cryptostrategy.platform.marketdata.api.model.CandleSubscription;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;
import com.cryptostrategy.platform.marketdata.api.model.RealtimeCandleQuery;

public interface MarketDataProvider {
    MarketProvider providerId();

    String normalizationVersion();

    HistoricalCandleBatch loadHistorical(HistoricalCandleQuery query);

    CandleSubscription subscribe(
            RealtimeCandleQuery query,
            CandleUpdateHandler updates,
            ConnectionStateHandler states);
}

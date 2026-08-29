package com.cryptostrategy.platform.marketdata.internal.provider.fixture;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.marketdata.api.event.CandleUpdateHandler;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionState;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionStateHandler;
import com.cryptostrategy.platform.marketdata.api.model.CandleSubscription;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;
import com.cryptostrategy.platform.marketdata.api.model.RealtimeCandleQuery;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketDataProvider;
import java.util.List;

public final class FixtureMarketDataProvider implements MarketDataProvider {
    public static final MarketProvider PROVIDER = new MarketProvider("FIXTURE");
    private final List<Candle> candles;
    public FixtureMarketDataProvider(List<Candle> candles) { this.candles = List.copyOf(candles); }
    @Override public MarketProvider providerId() { return PROVIDER; }
    @Override public String normalizationVersion() { return "fixture-v1"; }
    @Override public HistoricalCandleBatch loadHistorical(HistoricalCandleQuery query) {
        return new HistoricalCandleBatch(candles.stream().filter(candle -> !candle.key().openTime().isBefore(query.startTime())
                && candle.key().openTime().isBefore(query.endTime())).toList());
    }
    @Override public CandleSubscription subscribe(RealtimeCandleQuery query, CandleUpdateHandler updates, ConnectionStateHandler states) {
        states.onState(ConnectionState.CONNECTING); states.onState(ConnectionState.CONNECTED);
        return () -> states.onState(ConnectionState.DISCONNECTED);
    }
}

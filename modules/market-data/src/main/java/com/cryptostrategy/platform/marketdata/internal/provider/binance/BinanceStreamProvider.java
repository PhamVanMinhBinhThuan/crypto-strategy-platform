package com.cryptostrategy.platform.marketdata.internal.provider.binance;

import com.cryptostrategy.platform.domain.api.market.CandleKey;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.event.CandleUpdate;
import com.cryptostrategy.platform.marketdata.api.event.CandleUpdateHandler;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionState;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionStateHandler;
import com.cryptostrategy.platform.marketdata.api.model.CandleSubscription;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;
import com.cryptostrategy.platform.marketdata.api.model.RealtimeCandleQuery;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketDataProvider;
import com.cryptostrategy.platform.marketdata.internal.provider.binance.transport.BinanceStreamTransport;
import com.cryptostrategy.platform.marketdata.internal.observability.CorrelationContext;
import java.util.HashMap;
import java.util.Map;

public final class BinanceStreamProvider implements MarketDataProvider {
    private final BinanceStreamTransport transport; private final BinanceCandleMapper mapper; private final String normalizationVersion;
    public BinanceStreamProvider(BinanceStreamTransport transport, BinanceCandleMapper mapper, String normalizationVersion) { this.transport = transport; this.mapper = mapper; this.normalizationVersion = normalizationVersion; }
    @Override public MarketProvider providerId() { return MarketProvider.BINANCE; }
    @Override public String normalizationVersion() { return normalizationVersion; }
    @Override public HistoricalCandleBatch loadHistorical(HistoricalCandleQuery query) { throw new UnsupportedOperationException("Historical transport is separate"); }
    @Override public CandleSubscription subscribe(RealtimeCandleQuery query, CandleUpdateHandler updates, ConnectionStateHandler states) {
        CorrelationContext correlation = CorrelationContext.capture();
        states.onState(ConnectionState.CONNECTING); Map<CandleKey, CandleUpdate> latest = new HashMap<>();
        CandleSubscription subscription = transport.subscribe(streamName(query), frame -> correlation.wrap(() -> {
            CandleUpdate incoming = mapper.mapStream(frame, query); CandleUpdate accepted;
            synchronized (latest) {
                CandleUpdate current = latest.get(incoming.candle().key());
                if (current != null && current.providerEventTime().equals(incoming.providerEventTime())
                        && !current.candle().canonicalContentEquals(incoming.candle())) throw new MarketDataException(MarketDataErrorCode.MARKET_DATA_INTEGRITY_CONFLICT, "Equal-order realtime conflict");
                if (current != null && !shouldReplace(current, incoming)) return;
                latest.put(incoming.candle().key(), incoming); accepted = incoming;
            }
            updates.onUpdate(accepted);
        }).run(), correlation.wrap(() -> states.onState(ConnectionState.RECONNECTING)));
        states.onState(ConnectionState.CONNECTED); return () -> { subscription.close(); states.onState(ConnectionState.DISCONNECTED); };
    }
    private static boolean shouldReplace(CandleUpdate current, CandleUpdate incoming) {
        if (current.candle().closed()) return false;
        if (incoming.candle().closed()) return true;
        return incoming.providerEventTime().isAfter(current.providerEventTime());
    }
    private static String streamName(RealtimeCandleQuery query) {
        return (query.tradingPair().baseAsset().symbol().value() + query.tradingPair().quoteAsset().symbol().value()).toLowerCase(java.util.Locale.ROOT)
                + "@kline_" + query.timeframe().code();
    }
}

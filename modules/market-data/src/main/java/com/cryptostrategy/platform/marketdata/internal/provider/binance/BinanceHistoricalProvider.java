package com.cryptostrategy.platform.marketdata.internal.provider.binance;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.CandleKey;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.marketdata.api.event.CandleUpdateHandler;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionStateHandler;
import com.cryptostrategy.platform.marketdata.api.model.CandleSubscription;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;
import com.cryptostrategy.platform.marketdata.api.model.RealtimeCandleQuery;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketDataProvider;
import com.cryptostrategy.platform.marketdata.internal.provider.binance.transport.BinanceRestTransport;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BinanceHistoricalProvider implements MarketDataProvider {
    private final BinanceRestTransport transport; private final BinanceCandleMapper mapper; private final BinanceErrorTranslator errors;
    private final BinanceRetryPolicy retries; private final String normalizationVersion; private final MarketDataProvider streamDelegate;
    public BinanceHistoricalProvider(BinanceRestTransport transport, BinanceCandleMapper mapper, String normalizationVersion, MarketDataProvider streamDelegate) {
        this(transport, mapper, normalizationVersion, streamDelegate, BinanceRetryPolicy.defaults());
    }
    public BinanceHistoricalProvider(BinanceRestTransport transport, BinanceCandleMapper mapper, String normalizationVersion,
            MarketDataProvider streamDelegate, BinanceRetryPolicy retries) {
        this.transport = transport; this.mapper = mapper; this.errors = new BinanceErrorTranslator(); this.normalizationVersion = normalizationVersion; this.streamDelegate = streamDelegate; this.retries = retries;
    }
    @Override public MarketProvider providerId() { return MarketProvider.BINANCE; }
    @Override public String normalizationVersion() { return normalizationVersion; }
    @Override public HistoricalCandleBatch loadHistorical(HistoricalCandleQuery query) {
        Map<CandleKey, Candle> candles = new LinkedHashMap<>(); long cursor = query.startTime().toEpochMilli(); int pages = 0;
        while (cursor < query.endTime().toEpochMilli()) {
            if (++pages > query.maxPages()) throw new com.cryptostrategy.platform.marketdata.api.error.MarketDataException(com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode.INVALID_MARKET_QUERY, "Historical page bound exceeded");
            long requestedCursor = cursor;
            BinanceRestTransport.Response response = retries.execute(() -> transport.getKlines(compact(query), query.timeframe().code(), requestedCursor, query.endTime().toEpochMilli() - 1, query.pageSize()));
            if (response.status() != 200) throw errors.translate(response.status());
            JsonNode root = mapper.parse(response.body()); if (!root.isArray()) throw new com.cryptostrategy.platform.marketdata.api.error.MarketDataException(com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode.MARKET_DATA_MAPPING_FAILED, "Expected Binance kline array");
            if (root.isEmpty()) break;
            long previous = cursor;
            for (JsonNode tuple : root) {
                Candle candle = mapper.mapHistorical(tuple, query);
                if (!candle.key().openTime().isBefore(query.endTime()) || !candle.closed()) continue;
                Candle existing = candles.putIfAbsent(candle.key(), candle);
                if (existing != null && !existing.canonicalContentEquals(candle)) throw new com.cryptostrategy.platform.marketdata.api.error.MarketDataException(com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode.MARKET_DATA_INTEGRITY_CONFLICT, "Conflicting provider Candle");
                cursor = Math.max(cursor, query.timeframe().next(candle.key().openTime()).toEpochMilli());
            }
            if (cursor <= previous) throw new com.cryptostrategy.platform.marketdata.api.error.MarketDataException(com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode.MARKET_DATA_GAP, "Provider pagination made no progress");
            if (root.size() < query.pageSize()) break;
        }
        List<Candle> ordered = new ArrayList<>(candles.values()); ordered.sort(java.util.Comparator.comparing(candle -> candle.key().openTime()));
        return new HistoricalCandleBatch(ordered);
    }
    @Override public CandleSubscription subscribe(RealtimeCandleQuery query, CandleUpdateHandler updates, ConnectionStateHandler states) {
        if (streamDelegate == null) throw new UnsupportedOperationException("Realtime transport is not configured");
        return streamDelegate.subscribe(query, updates, states);
    }
    private static String compact(HistoricalCandleQuery query) { return query.tradingPair().baseAsset().symbol().value() + query.tradingPair().quoteAsset().symbol().value(); }
}

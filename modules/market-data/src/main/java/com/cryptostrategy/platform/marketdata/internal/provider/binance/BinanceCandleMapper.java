package com.cryptostrategy.platform.marketdata.internal.provider.binance;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.CandleKey;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.event.CandleUpdate;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;

public final class BinanceCandleMapper {
    private final ObjectMapper mapper;
    public BinanceCandleMapper(ObjectMapper mapper) { this.mapper = mapper; }
    public Candle mapHistorical(JsonNode tuple, HistoricalCandleQuery query) {
        try {
            if (!tuple.isArray() || tuple.size() < 7) throw new IllegalArgumentException("Malformed kline tuple");
            Instant openTime = Instant.ofEpochMilli(tuple.get(0).longValue());
            Instant closeTime = query.timeframe().next(openTime);
            if (tuple.get(6).longValue() != closeTime.toEpochMilli() - 1) throw new IllegalArgumentException("Unexpected Binance close boundary");
            boolean closed = !closeTime.isAfter(query.collectionCutoff());
            return new Candle(new CandleKey(MarketProvider.BINANCE, query.tradingPair(), query.timeframe(), openTime), closeTime,
                    decimal(tuple.get(1)), decimal(tuple.get(2)), decimal(tuple.get(3)), decimal(tuple.get(4)), decimal(tuple.get(5)), closed);
        } catch (RuntimeException exception) {
            if (exception instanceof MarketDataException marketDataException) throw marketDataException;
            throw new MarketDataException(MarketDataErrorCode.MARKET_DATA_MAPPING_FAILED, "Binance kline mapping failed", java.util.Map.of(), exception);
        }
    }
    public CandleUpdate mapStream(String payload, com.cryptostrategy.platform.marketdata.api.model.RealtimeCandleQuery query) {
        try {
            JsonNode root = mapper.readTree(payload); JsonNode event = root.has("data") ? root.get("data") : root; JsonNode kline = event.get("k");
            if (kline == null || !kline.get("i").asText().equals(query.timeframe().code())) throw new IllegalArgumentException("Unexpected stream interval");
            String expectedSymbol = query.tradingPair().baseAsset().symbol().value() + query.tradingPair().quoteAsset().symbol().value();
            JsonNode symbol = event.get("s");
            if (symbol == null || !expectedSymbol.equals(symbol.asText())) throw new IllegalArgumentException("Unexpected stream symbol");
            Instant open = Instant.ofEpochMilli(kline.get("t").longValue()); Instant close = query.timeframe().next(open);
            if (kline.get("T").longValue() != close.toEpochMilli() - 1) throw new IllegalArgumentException("Unexpected stream close boundary");
            Candle candle = new Candle(new CandleKey(MarketProvider.BINANCE, query.tradingPair(), query.timeframe(), open), close,
                    decimal(kline.get("o")), decimal(kline.get("h")), decimal(kline.get("l")), decimal(kline.get("c")), decimal(kline.get("v")), kline.get("x").booleanValue());
            return new CandleUpdate(candle, Instant.ofEpochMilli(event.get("E").longValue()));
        } catch (Exception exception) { throw new MarketDataException(MarketDataErrorCode.MARKET_DATA_MAPPING_FAILED, "Binance stream mapping failed", java.util.Map.of(), exception); }
    }
    public JsonNode parse(String body) { try { return mapper.readTree(body); } catch (Exception exception) { throw new MarketDataException(MarketDataErrorCode.MARKET_DATA_MAPPING_FAILED, "Binance payload mapping failed", java.util.Map.of(), exception); } }
    private static BigDecimal decimal(JsonNode node) { return new BigDecimal(node.asText()); }
}

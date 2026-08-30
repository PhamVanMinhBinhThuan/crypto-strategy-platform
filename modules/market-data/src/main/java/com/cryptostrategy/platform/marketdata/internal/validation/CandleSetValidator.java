package com.cryptostrategy.platform.marketdata.internal.validation;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.CandleKey;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CandleSetValidator {
    private CandleSetValidator() { }

    public static List<Candle> normalizeComplete(HistoricalCandleQuery query, List<Candle> input) {
        Map<CandleKey, Candle> unique = new LinkedHashMap<>();
        for (Candle candle : input) {
            validateScope(query, candle);
            Candle accepted = unique.putIfAbsent(candle.key(), candle);
            if (accepted != null && !accepted.canonicalContentEquals(candle)) {
                throw new MarketDataException(MarketDataErrorCode.MARKET_DATA_INTEGRITY_CONFLICT, "Conflicting Candle identity", scope(candle));
            }
        }
        List<Candle> ordered = new ArrayList<>(unique.values());
        ordered.sort(Comparator.comparing(candle -> candle.key().openTime()));
        Instant expected = query.startTime();
        for (Candle candle : ordered) {
            if (!candle.key().openTime().equals(expected)) throw new MarketDataException(MarketDataErrorCode.MARKET_DATA_GAP, "Historical range contains a gap");
            expected = query.timeframe().next(expected);
        }
        if (!expected.equals(query.endTime())) throw new MarketDataException(MarketDataErrorCode.MARKET_DATA_GAP, "Historical range is incomplete");
        return List.copyOf(ordered);
    }

    private static void validateScope(HistoricalCandleQuery query, Candle candle) {
        if (!candle.closed() || candle.closeTime().isAfter(query.collectionCutoff())) throw new MarketDataException(MarketDataErrorCode.INVALID_MARKET_QUERY, "Candle closure is not proven");
        if (!candle.key().provider().equals(query.provider()) || !candle.key().tradingPair().tradingPairId().equals(query.tradingPair().tradingPairId())
                || candle.key().timeframe() != query.timeframe() || candle.key().openTime().isBefore(query.startTime())
                || !candle.key().openTime().isBefore(query.endTime())) throw new MarketDataException(MarketDataErrorCode.MARKET_DATA_INTEGRITY_CONFLICT, "Candle is outside Dataset scope");
    }

    private static Map<String, String> scope(Candle candle) {
        return Map.of("provider", candle.key().provider().value(), "pair", candle.key().tradingPair().canonicalSymbol(),
                "timeframe", candle.key().timeframe().code(), "openTime", candle.key().openTime().toString());
    }
}

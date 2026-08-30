package com.cryptostrategy.platform.marketdata.support;

import com.cryptostrategy.platform.domain.api.market.*;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public final class MarketFixtures {
    public static final TradingPair PAIR = new TradingPair(new TradingPairId("01ARZ3NDEKTSV4RRFFQ69G5FAX"),
            new Asset(new AssetId("01ARZ3NDEKTSV4RRFFQ69G5FAV"), new AssetSymbol("BTC"), Optional.empty(), true),
            new Asset(new AssetId("01ARZ3NDEKTSV4RRFFQ69G5FAW"), new AssetSymbol("USDT"), Optional.empty(), true), true);
    public static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private MarketFixtures() { }
    public static Candle candle(int minute, String close) {
        Instant open = START.plusSeconds(60L * minute);
        return new Candle(new CandleKey(MarketProvider.BINANCE, PAIR, Timeframe.ONE_MINUTE, open), open.plusSeconds(60),
                new BigDecimal("1"), new BigDecimal("3"), new BigDecimal("0.5"), new BigDecimal(close), new BigDecimal("10"), true);
    }
    public static HistoricalCandleQuery query(int count) {
        return new HistoricalCandleQuery(MarketProvider.BINANCE, PAIR, Timeframe.ONE_MINUTE, START,
                START.plusSeconds(60L * count), START.plusSeconds(60L * count), 1000, 10);
    }
}

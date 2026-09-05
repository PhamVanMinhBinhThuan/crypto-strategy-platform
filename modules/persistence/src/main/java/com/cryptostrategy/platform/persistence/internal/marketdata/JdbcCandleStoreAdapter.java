package com.cryptostrategy.platform.persistence.internal.marketdata;

import com.cryptostrategy.platform.domain.api.market.*;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.model.PersistedCandle;
import com.cryptostrategy.platform.marketdata.api.port.out.ClosedCandleStore;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcCandleStoreAdapter implements ClosedCandleStore {
    private final JdbcTemplate jdbc; private final JdbcMarketReferenceDataAdapter references;
    public JdbcCandleStoreAdapter(JdbcTemplate jdbc, JdbcMarketReferenceDataAdapter references) { this.jdbc = jdbc; this.references = references; }
    @Override public PersistedCandle saveClosed(Candle candle) {
        if (!candle.closed()) throw new MarketDataException(MarketDataErrorCode.INVALID_MARKET_QUERY, "Only closed Candles may persist");
        TradingPair pair = references.resolveTradingPair(candle.key().tradingPair()); CandleId id = CandleId.generate();
        jdbc.update(MarketDataSql.INSERT_CANDLE, id.value(), candle.key().provider().value(), pair.tradingPairId().value(), candle.key().timeframe().code(),
                Timestamp.from(candle.key().openTime()), Timestamp.from(candle.closeTime()), candle.open(), candle.high(), candle.low(), candle.close(), candle.volume());
        PersistedCandle stored = findKey(candle, pair);
        if (!stored.candle().canonicalContentEquals(withPair(candle, pair))) throw new MarketDataException(MarketDataErrorCode.MARKET_DATA_INTEGRITY_CONFLICT, "Accepted Candle content differs");
        return stored;
    }
    @Override public List<PersistedCandle> saveClosedBatch(List<Candle> candles) { return candles.stream().sorted(Comparator.comparing(value -> value.key().openTime())).map(this::saveClosed).toList(); }
    @Override public List<PersistedCandle> findRange(MarketProvider provider, TradingPairId pairId, Timeframe timeframe, Instant start, Instant end) {
        return jdbc.query(MarketDataSql.FIND_CANDLE_RANGE, (rs, row) -> MarketDataRows.candle(rs), provider.value(), pairId.value(), timeframe.code(), Timestamp.from(start), Timestamp.from(end));
    }
    private PersistedCandle findKey(Candle candle, TradingPair pair) { return jdbc.query(MarketDataSql.FIND_CANDLE_KEY, (rs, row) -> MarketDataRows.candle(rs), candle.key().provider().value(), pair.tradingPairId().value(), candle.key().timeframe().code(), Timestamp.from(candle.key().openTime())).stream().findFirst().orElseThrow(); }
    private static Candle withPair(Candle candle, TradingPair pair) { return new Candle(new CandleKey(candle.key().provider(), pair, candle.key().timeframe(), candle.key().openTime()), candle.closeTime(), candle.open(), candle.high(), candle.low(), candle.close(), candle.volume(), candle.closed()); }
}

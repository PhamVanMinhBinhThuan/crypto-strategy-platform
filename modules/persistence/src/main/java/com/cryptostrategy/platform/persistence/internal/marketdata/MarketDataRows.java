package com.cryptostrategy.platform.persistence.internal.marketdata;

import com.cryptostrategy.platform.domain.api.market.*;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.marketdata.api.model.PersistedCandle;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

final class MarketDataRows {
    private MarketDataRows() { }
    static Asset asset(ResultSet rs, String prefix) throws SQLException {
        String name = rs.getString(prefix + "name");
        return new Asset(new AssetId(rs.getString(prefix + "id")), new AssetSymbol(rs.getString(prefix + "symbol")), Optional.ofNullable(name), rs.getBoolean(prefix + "active"));
    }
    static TradingPair pair(ResultSet rs) throws SQLException {
        return new TradingPair(new TradingPairId(rs.getString("trading_pair_id")), asset(rs, "ba_"), asset(rs, "qa_"), rs.getBoolean("tp_active"));
    }
    static PersistedCandle candle(ResultSet rs) throws SQLException {
        TradingPair pair = pair(rs); Timeframe timeframe = Timeframe.fromCode(rs.getString("timeframe")); Instant open = rs.getObject("open_time", java.time.OffsetDateTime.class).toInstant();
        Candle candle = new Candle(new CandleKey(new MarketProvider(rs.getString("provider")), pair, timeframe, open), rs.getObject("close_time", java.time.OffsetDateTime.class).toInstant(),
                rs.getBigDecimal("open"), rs.getBigDecimal("high"), rs.getBigDecimal("low"), rs.getBigDecimal("close"), rs.getBigDecimal("volume"), true);
        return new PersistedCandle(new CandleId(rs.getString("candle_id")), candle);
    }
    static DatasetSnapshot dataset(ResultSet rs) throws SQLException {
        return new DatasetSnapshot(new DatasetVersionId(rs.getString("dataset_version_id")), rs.getString("version"), new MarketProvider(rs.getString("provider")), pair(rs),
                Timeframe.fromCode(rs.getString("timeframe")), rs.getString("normalization_version"), rs.getObject("range_start", java.time.OffsetDateTime.class).toInstant(),
                rs.getObject("range_end", java.time.OffsetDateTime.class).toInstant(), rs.getInt("candle_count"), rs.getString("checksum"), rs.getObject("created_at", java.time.OffsetDateTime.class).toInstant());
    }
}

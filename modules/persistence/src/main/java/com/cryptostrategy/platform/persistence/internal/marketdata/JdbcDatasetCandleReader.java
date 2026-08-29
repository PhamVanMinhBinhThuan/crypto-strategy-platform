package com.cryptostrategy.platform.persistence.internal.marketdata;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.model.CandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.DatasetMembership;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetCandleReader;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcDatasetCandleReader implements DatasetCandleReader {
    private final JdbcTemplate jdbc;
    public JdbcDatasetCandleReader(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public CandleBatch readCandles(DatasetVersionId datasetId, int fromSequence, int batchSize) {
        if (fromSequence < 0 || batchSize < 1 || batchSize > CandleBatch.MAX_BATCH_SIZE) throw new MarketDataException(MarketDataErrorCode.INVALID_MARKET_QUERY, "Invalid Dataset page");
        List<Integer> counts = jdbc.query("select candle_count from market.dataset_version where dataset_version_id=?", (rs, row) -> rs.getInt(1), datasetId.value());
        if (counts.isEmpty()) throw new MarketDataException(MarketDataErrorCode.DATASET_NOT_FOUND, "Dataset not found");
        int count = counts.getFirst(); if (fromSequence > count) throw new MarketDataException(MarketDataErrorCode.INVALID_MARKET_QUERY, "Dataset sequence is beyond end");
        if (fromSequence == count) return new CandleBatch(datasetId, fromSequence, List.of(), fromSequence, false);
        List<DatasetMembership> fetched = jdbc.query(MarketDataSql.READ_MEMBERS, (rs, row) -> new DatasetMembership(datasetId, rs.getInt("sequence_no"), MarketDataRows.candle(rs)), datasetId.value(), fromSequence, batchSize + 1);
        boolean more = fetched.size() > batchSize; List<DatasetMembership> members = more ? new ArrayList<>(fetched.subList(0, batchSize)) : fetched;
        return new CandleBatch(datasetId, fromSequence, members, fromSequence + members.size(), more);
    }
}

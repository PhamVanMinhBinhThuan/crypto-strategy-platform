package com.cryptostrategy.platform.persistence.internal.marketdata;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.model.CandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.DatasetFinalization;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.marketdata.api.model.PersistedCandle;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetStore;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public final class JdbcDatasetStoreAdapter implements DatasetStore {
    private final JdbcTemplate jdbc; private final TransactionTemplate transactions; private final JdbcMarketReferenceDataAdapter references;
    private final JdbcCandleStoreAdapter candles; private final JdbcDatasetCandleReader reader;
    public JdbcDatasetStoreAdapter(JdbcTemplate jdbc, TransactionTemplate transactions, JdbcMarketReferenceDataAdapter references,
                                   JdbcCandleStoreAdapter candles, JdbcDatasetCandleReader reader) {
        this.jdbc = jdbc; this.transactions = transactions; this.references = references; this.candles = candles; this.reader = reader;
    }
    @Override public DatasetSnapshot finalizeAtomically(DatasetFinalization finalization) {
        try {
            return transactions.execute(status -> insert(finalization));
        } catch (DuplicateKeyException duplicate) {
            DatasetSnapshot winner = findByChecksum(finalization.snapshot().checksum()).orElseThrow(() -> duplicate);
            if (!equivalent(winner, finalization)) throw new MarketDataException(MarketDataErrorCode.MARKET_DATA_INTEGRITY_CONFLICT, "Dataset checksum provenance conflict");
            return winner;
        }
    }
    private DatasetSnapshot insert(DatasetFinalization finalization) {
        DatasetSnapshot snapshot = finalization.snapshot(); TradingPair pair = references.resolveTradingPair(snapshot.tradingPair());
        List<PersistedCandle> persisted = candles.saveClosedBatch(finalization.candles());
        jdbc.update(MarketDataSql.INSERT_DATASET, snapshot.datasetVersionId().value(), snapshot.version(), snapshot.provider().value(), pair.tradingPairId().value(), snapshot.timeframe().code(), snapshot.normalizationVersion(),
                Timestamp.from(snapshot.rangeStart()), Timestamp.from(snapshot.rangeEnd()), snapshot.candleCount(), snapshot.checksum(), Timestamp.from(snapshot.createdAt()));
        for (int sequence = 0; sequence < persisted.size(); sequence++) jdbc.update(MarketDataSql.INSERT_MEMBER, snapshot.datasetVersionId().value(), sequence, persisted.get(sequence).candleId().value());
        return find(snapshot.datasetVersionId()).orElseThrow();
    }
    @Override public Optional<DatasetSnapshot> find(DatasetVersionId datasetId) { return jdbc.query(MarketDataSql.FIND_DATASET_ID, (rs, row) -> MarketDataRows.dataset(rs), datasetId.value()).stream().findFirst(); }
    @Override public Optional<DatasetSnapshot> findByChecksum(String checksum) { return jdbc.query(MarketDataSql.FIND_DATASET_CHECKSUM, (rs, row) -> MarketDataRows.dataset(rs), checksum).stream().findFirst(); }
    private boolean equivalent(DatasetSnapshot winner, DatasetFinalization expected) {
        DatasetSnapshot candidate = expected.snapshot();
        if (!winner.version().equals(candidate.version()) || !winner.normalizationVersion().equals(candidate.normalizationVersion())
                || !winner.provider().equals(candidate.provider()) || !winner.tradingPair().tradingPairId().equals(candidate.tradingPair().tradingPairId())
                || winner.timeframe() != candidate.timeframe() || !winner.rangeStart().equals(candidate.rangeStart()) || !winner.rangeEnd().equals(candidate.rangeEnd())
                || winner.candleCount() != candidate.candleCount() || !winner.checksum().equals(candidate.checksum())) return false;
        int sequence = 0;
        while (sequence < winner.candleCount()) {
            CandleBatch batch = reader.readCandles(winner.datasetVersionId(), sequence,
                    Math.min(CandleBatch.MAX_BATCH_SIZE, winner.candleCount() - sequence));
            if (batch.members().isEmpty()) return false;
            for (var member : batch.members()) {
                if (sequence >= expected.candles().size()
                        || !member.candle().candle().canonicalContentEquals(expected.candles().get(sequence))) return false;
                sequence++;
            }
        }
        return sequence == expected.candles().size();
    }
}

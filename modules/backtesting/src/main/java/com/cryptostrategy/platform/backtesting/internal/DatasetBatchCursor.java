package com.cryptostrategy.platform.backtesting.internal;

import com.cryptostrategy.platform.backtesting.api.error.*;
import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.marketdata.api.model.*;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetCandleReader;
import java.util.Objects;

/** Traverses one bounded CandleBatch at a time and enforces contiguous progress. */
final class DatasetBatchCursor {
    private final ResolvedBacktestRun command;
    private final DatasetCandleReader reader;
    private final BacktestInputValidator validator = new BacktestInputValidator();

    DatasetBatchCursor(ResolvedBacktestRun command, DatasetCandleReader reader) {
        this.command=Objects.requireNonNull(command);this.reader=Objects.requireNonNull(reader);
    }

    void forEach(CandleConsumer consumer) {
        int expected=0; Candle previous=null;
        while (true) {
            CandleBatch batch;
            try { batch=reader.readCandles(command.dataset().datasetVersionId(),expected,command.batchSize()); }
            catch (RuntimeException error) { throw new BacktestException(BacktestErrorCode.INVALID_BATCH,error.getMessage()); }
            if (!batch.datasetId().equals(command.dataset().datasetVersionId()) || batch.fromSequence()!=expected
                    || batch.members().size()>command.batchSize() || (batch.members().isEmpty()&&batch.hasMore())) {
                throw new BacktestException(BacktestErrorCode.INVALID_BATCH,"Invalid batch progression");
            }
            for (int index=0;index<batch.members().size();index++) {
                DatasetMembership member=batch.members().get(index);Candle candle=member.candle().candle();
                validator.validate(command,candle,member,expected,previous);previous=candle;
                consumer.accept(candle,!batch.hasMore()&&index==batch.members().size()-1);expected++;
            }
            if (!batch.hasMore()) {
                if (batch.nextSequence()!=expected || expected!=command.dataset().candleCount())
                    throw new BacktestException(BacktestErrorCode.INVALID_DATASET,"Candle count mismatch");
                return;
            }
            if (batch.nextSequence()!=expected) throw new BacktestException(BacktestErrorCode.INVALID_BATCH,"Batch did not advance contiguously");
        }
    }

    @FunctionalInterface interface CandleConsumer { void accept(Candle candle, boolean last); }
}

package com.cryptostrategy.platform.marketdata.api.port.out;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.model.CandleBatch;

@FunctionalInterface
public interface DatasetCandleReader {
    CandleBatch readCandles(DatasetVersionId datasetId, int fromSequence, int batchSize);
}

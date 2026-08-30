package com.cryptostrategy.platform.marketdata.api.model;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import java.util.Objects;

public record DatasetMembership(DatasetVersionId datasetVersionId, int sequenceNo, PersistedCandle candle) {
    public DatasetMembership { Objects.requireNonNull(datasetVersionId); Objects.requireNonNull(candle); if (sequenceNo < 0) throw new IllegalArgumentException("sequenceNo"); }
}

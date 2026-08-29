package com.cryptostrategy.platform.marketdata.api.model;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import java.util.List;
import java.util.Objects;

public record CandleBatch(DatasetVersionId datasetId, int fromSequence, List<DatasetMembership> members,
                          int nextSequence, boolean hasMore) {
    public static final int MAX_BATCH_SIZE = 5000;
    public CandleBatch {
        Objects.requireNonNull(datasetId); members = List.copyOf(members);
        if (fromSequence < 0 || nextSequence < fromSequence || nextSequence != fromSequence + members.size()) throw new IllegalArgumentException("Invalid Candle batch");
        for (int index = 0; index < members.size(); index++) if (members.get(index).sequenceNo() != fromSequence + index) throw new IllegalArgumentException("Non-contiguous batch");
    }
}

package com.cryptostrategy.platform.news.api.port.in;

import com.cryptostrategy.platform.news.api.model.SentimentSnapshot;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotId;

public interface GetSentimentSnapshotUseCase {
    SentimentSnapshot get(SentimentSnapshotId snapshotId);
}

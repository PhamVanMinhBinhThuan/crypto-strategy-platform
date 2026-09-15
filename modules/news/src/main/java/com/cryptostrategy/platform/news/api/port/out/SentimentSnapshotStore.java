package com.cryptostrategy.platform.news.api.port.out;

import com.cryptostrategy.platform.news.api.model.SentimentSnapshot;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotId;
import java.util.Optional;

public interface SentimentSnapshotStore {
    SentimentSnapshot save(SentimentSnapshot snapshot);
    Optional<SentimentSnapshot> find(SentimentSnapshotId snapshotId);
}

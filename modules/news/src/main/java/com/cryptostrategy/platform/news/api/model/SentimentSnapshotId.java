package com.cryptostrategy.platform.news.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record SentimentSnapshotId(String value) implements UlidIdentifier {
    public SentimentSnapshotId {
        value = Ulids.requireValid(value);
    }

    public static SentimentSnapshotId generate() {
        return new SentimentSnapshotId(Ulids.generate());
    }

    @Override public String toString() { return value; }
}

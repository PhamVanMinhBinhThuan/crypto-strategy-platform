package com.cryptostrategy.platform.strategy.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record StrategyInputSnapshotId(String value) implements UlidIdentifier {
    public StrategyInputSnapshotId {
        value = Ulids.requireValid(value);
    }
}

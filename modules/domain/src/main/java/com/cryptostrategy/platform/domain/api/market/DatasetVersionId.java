package com.cryptostrategy.platform.domain.api.market;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record DatasetVersionId(String value) implements UlidIdentifier {
    public DatasetVersionId { value = Ulids.requireValid(value); }
    public static DatasetVersionId generate() { return new DatasetVersionId(Ulids.generate()); }
    @Override public String toString() { return value; }
}

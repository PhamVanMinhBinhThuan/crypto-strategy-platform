package com.cryptostrategy.platform.domain.api.market;

import com.cryptostrategy.platform.domain.internal.identity.MarketUlid;

public record DatasetVersionId(String value) {
    public DatasetVersionId { value = MarketUlid.requireValid(value); }
    public static DatasetVersionId generate() { return new DatasetVersionId(MarketUlid.generate()); }
    @Override public String toString() { return value; }
}

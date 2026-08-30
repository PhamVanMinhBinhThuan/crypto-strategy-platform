package com.cryptostrategy.platform.domain.api.market;

import com.cryptostrategy.platform.domain.internal.identity.MarketUlid;

public record AssetId(String value) {
    public AssetId { value = MarketUlid.requireValid(value); }
    public static AssetId generate() { return new AssetId(MarketUlid.generate()); }
    @Override public String toString() { return value; }
}

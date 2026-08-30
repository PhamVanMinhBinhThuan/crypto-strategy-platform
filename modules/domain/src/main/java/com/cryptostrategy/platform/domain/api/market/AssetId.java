package com.cryptostrategy.platform.domain.api.market;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record AssetId(String value) implements UlidIdentifier {
    public AssetId { value = Ulids.requireValid(value); }
    public static AssetId generate() { return new AssetId(Ulids.generate()); }
    @Override public String toString() { return value; }
}

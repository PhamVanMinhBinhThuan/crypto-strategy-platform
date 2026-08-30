package com.cryptostrategy.platform.domain.api.market;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record TradingPairId(String value) implements UlidIdentifier {
    public TradingPairId { value = Ulids.requireValid(value); }
    public static TradingPairId generate() { return new TradingPairId(Ulids.generate()); }
    @Override public String toString() { return value; }
}

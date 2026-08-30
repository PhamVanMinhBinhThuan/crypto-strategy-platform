package com.cryptostrategy.platform.domain.api.market;

import com.cryptostrategy.platform.domain.internal.identity.MarketUlid;

public record TradingPairId(String value) {
    public TradingPairId { value = MarketUlid.requireValid(value); }
    public static TradingPairId generate() { return new TradingPairId(MarketUlid.generate()); }
    @Override public String toString() { return value; }
}

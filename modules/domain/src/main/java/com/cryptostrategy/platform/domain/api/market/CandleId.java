package com.cryptostrategy.platform.domain.api.market;

import com.cryptostrategy.platform.domain.internal.identity.MarketUlid;

public record CandleId(String value) {
    public CandleId { value = MarketUlid.requireValid(value); }
    public static CandleId generate() { return new CandleId(MarketUlid.generate()); }
    @Override public String toString() { return value; }
}

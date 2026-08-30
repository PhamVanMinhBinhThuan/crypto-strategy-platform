package com.cryptostrategy.platform.domain.api.market;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record CandleId(String value) implements UlidIdentifier {
    public CandleId { value = Ulids.requireValid(value); }
    public static CandleId generate() { return new CandleId(Ulids.generate()); }
    @Override public String toString() { return value; }
}

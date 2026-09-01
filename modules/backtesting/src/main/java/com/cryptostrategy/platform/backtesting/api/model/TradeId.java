package com.cryptostrategy.platform.backtesting.api.model;
import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;
public record TradeId(String value) implements UlidIdentifier {
    public TradeId { value = Ulids.requireValid(value); }
    public static TradeId generate() { return new TradeId(Ulids.generate()); }
    @Override public String toString() { return value; }
}

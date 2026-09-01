package com.cryptostrategy.platform.backtesting.api.model;
import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;
public record BacktestResultId(String value) implements UlidIdentifier {
    public BacktestResultId { value = Ulids.requireValid(value); }
    public static BacktestResultId generate() { return new BacktestResultId(Ulids.generate()); }
    @Override public String toString() { return value; }
}

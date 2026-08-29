package com.cryptostrategy.platform.domain.api.market;

import java.util.Objects;
import java.util.regex.Pattern;

public record MarketProvider(String value) {
    private static final Pattern FORMAT = Pattern.compile("^[A-Z][A-Z0-9_-]*$");
    public static final MarketProvider BINANCE = new MarketProvider("BINANCE");
    public MarketProvider {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) throw new IllegalArgumentException("Provider must be canonical uppercase");
    }
    @Override public String toString() { return value; }
}

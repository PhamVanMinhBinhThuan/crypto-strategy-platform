package com.cryptostrategy.platform.domain.api.market;

import java.util.Objects;
import java.util.regex.Pattern;

public record AssetSymbol(String value) {
    private static final Pattern FORMAT = Pattern.compile("^[A-Z0-9]+$");
    public AssetSymbol {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) throw new IllegalArgumentException("Asset symbol must be uppercase alphanumeric");
    }
    @Override public String toString() { return value; }
}

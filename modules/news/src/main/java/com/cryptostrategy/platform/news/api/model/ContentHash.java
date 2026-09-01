package com.cryptostrategy.platform.news.api.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record ContentHash(String value) {
    private static final Pattern FORMAT = Pattern.compile("sha256:[0-9a-f]{64}");
    public ContentHash {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) throw new IllegalArgumentException("Invalid SHA-256 content hash");
    }
    @Override public String toString() { return value; }
}

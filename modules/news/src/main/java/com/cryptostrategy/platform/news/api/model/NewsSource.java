package com.cryptostrategy.platform.news.api.model;

import java.util.Objects;

public record NewsSource(String value) {
    public NewsSource {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty() || value.length() > 200) throw new IllegalArgumentException("Invalid News source");
    }
    @Override public String toString() { return value; }
}

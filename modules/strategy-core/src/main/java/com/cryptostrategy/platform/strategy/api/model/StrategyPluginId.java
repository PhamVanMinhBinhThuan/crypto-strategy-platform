package com.cryptostrategy.platform.strategy.api.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record StrategyPluginId(String value) implements Comparable<StrategyPluginId> {
    private static final Pattern FORMAT = Pattern.compile("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$");
    public StrategyPluginId {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) throw new IllegalArgumentException("Invalid Strategy plugin slug");
    }
    @Override public int compareTo(StrategyPluginId other) { return value.compareTo(other.value); }
    @Override public String toString() { return value; }
}

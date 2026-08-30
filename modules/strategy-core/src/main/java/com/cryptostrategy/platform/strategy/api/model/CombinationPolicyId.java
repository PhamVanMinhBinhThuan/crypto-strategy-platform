package com.cryptostrategy.platform.strategy.api.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record CombinationPolicyId(String value) implements Comparable<CombinationPolicyId> {
    private static final Pattern FORMAT = Pattern.compile("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$");
    public CombinationPolicyId {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) throw new IllegalArgumentException("Invalid combination policy slug");
    }
    @Override public int compareTo(CombinationPolicyId other) { return value.compareTo(other.value); }
    @Override public String toString() { return value; }
}

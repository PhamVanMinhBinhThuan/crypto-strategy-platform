package com.cryptostrategy.platform.search.api.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record GeneratorId(String value) implements Comparable<GeneratorId> {
    private static final Pattern FORMAT = Pattern.compile("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$");

    public GeneratorId {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Generator ID must be a lowercase kebab-case slug");
        }
    }

    @Override
    public int compareTo(GeneratorId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}

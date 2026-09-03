package com.cryptostrategy.platform.search.api.model;

import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import java.util.Objects;

public record GeneratorVersion(SemanticVersion value) implements Comparable<GeneratorVersion> {
    public GeneratorVersion {
        Objects.requireNonNull(value, "value");
    }

    public static GeneratorVersion parse(String value) {
        return new GeneratorVersion(SemanticVersion.parse(value));
    }

    @Override
    public int compareTo(GeneratorVersion other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

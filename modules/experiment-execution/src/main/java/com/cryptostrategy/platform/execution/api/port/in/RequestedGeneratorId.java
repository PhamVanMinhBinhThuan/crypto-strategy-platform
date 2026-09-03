package com.cryptostrategy.platform.execution.api.port.in;

import java.util.Objects;

/** Typed public generator slug before it is resolved against the Search registry. */
public record RequestedGeneratorId(String value) {
    public RequestedGeneratorId {
        value = Objects.requireNonNull(value, "value");
        if (!value.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$")) {
            throw new IllegalArgumentException("generatorId must be a lowercase slug");
        }
    }
}

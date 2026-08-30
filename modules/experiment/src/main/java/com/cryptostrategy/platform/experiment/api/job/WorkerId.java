package com.cryptostrategy.platform.experiment.api.job;

import java.util.Objects;

public record WorkerId(String value) {
    public WorkerId {
        Objects.requireNonNull(value, "workerId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("workerId cannot be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

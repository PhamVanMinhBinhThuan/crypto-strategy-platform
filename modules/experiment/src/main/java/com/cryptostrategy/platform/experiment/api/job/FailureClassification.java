package com.cryptostrategy.platform.experiment.api.job;

public enum FailureClassification {
    TRANSIENT,
    DETERMINISTIC;

    public boolean isRetryable() {
        return this == TRANSIENT;
    }
}

package com.cryptostrategy.platform.experiment.api.job;

public enum FailureClassification {
    TRANSIENT_NETWORK_ERROR(true),
    DATA_UNAVAILABLE_RETRY(true),
    WORKER_CRASHED(true),
    PERMANENT_LOGIC_ERROR(false),
    UNKNOWN_ERROR(false);

    private final boolean retryable;

    FailureClassification(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}

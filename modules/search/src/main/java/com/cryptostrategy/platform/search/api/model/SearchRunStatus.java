package com.cryptostrategy.platform.search.api.model;

public enum SearchRunStatus {
    PENDING,
    RUNNING,
    STOPPING,
    COMPLETED,
    STOPPED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == STOPPED || this == FAILED;
    }
}

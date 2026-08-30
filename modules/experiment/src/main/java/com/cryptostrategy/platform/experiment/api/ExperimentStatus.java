package com.cryptostrategy.platform.experiment.api;

public enum ExperimentStatus {
    CREATED,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    STOP_REQUESTED,
    STOPPED;

    public boolean isMutable() {
        return this == CREATED;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == STOPPED;
    }
}

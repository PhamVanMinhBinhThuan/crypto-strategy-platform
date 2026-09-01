package com.cryptostrategy.platform.experiment.api.job;

public enum JobStatus {
    QUEUED,
    RUNNING,
    RETRY_SCHEDULED,
    SUCCEEDED,
    FAILED,
    CANCEL_REQUESTED,
    CANCELLED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED;
    }
}

package com.cryptostrategy.platform.experiment.api.error;

public abstract class ExperimentException extends RuntimeException {
    protected ExperimentException(String message) {
        super(message);
    }

    protected ExperimentException(String message, Throwable cause) {
        super(message, cause);
    }
}

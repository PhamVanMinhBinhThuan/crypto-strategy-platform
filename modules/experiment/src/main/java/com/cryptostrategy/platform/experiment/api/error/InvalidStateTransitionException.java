package com.cryptostrategy.platform.experiment.api.error;

public class InvalidStateTransitionException extends ExperimentException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}

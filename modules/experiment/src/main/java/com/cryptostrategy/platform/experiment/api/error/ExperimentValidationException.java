package com.cryptostrategy.platform.experiment.api.error;

public class ExperimentValidationException extends ExperimentException {
    public ExperimentValidationException(String message) {
        super(message);
    }

    public ExperimentValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

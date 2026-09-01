package com.cryptostrategy.platform.experiment.api.error;

public class IdempotencyConflictException extends ExperimentException {
    public IdempotencyConflictException(String message) {
        super(message);
    }

    public IdempotencyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

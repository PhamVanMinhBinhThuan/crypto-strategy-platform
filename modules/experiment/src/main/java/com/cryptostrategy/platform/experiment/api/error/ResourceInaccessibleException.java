package com.cryptostrategy.platform.experiment.api.error;

public class ResourceInaccessibleException extends ExperimentException {
    public ResourceInaccessibleException(String message) {
        super(message);
    }

    public ResourceInaccessibleException(String message, Throwable cause) {
        super(message, cause);
    }
}

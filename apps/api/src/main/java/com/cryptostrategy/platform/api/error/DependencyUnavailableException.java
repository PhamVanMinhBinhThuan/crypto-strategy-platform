package com.cryptostrategy.platform.api.error;

/** Signals that a documented operation is intentionally gated on an unavailable owner capability. */
public final class DependencyUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DependencyUnavailableException(String dependency) {
        super(dependency);
    }
}

package com.cryptostrategy.platform.api.error;

/** Conceals whether a private resource is missing or belongs to another user. */
public final class ResourceInaccessibleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ResourceInaccessibleException() {
        super("The requested resource was not found.");
    }
}

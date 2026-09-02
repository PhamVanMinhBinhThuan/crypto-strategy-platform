package com.cryptostrategy.platform.api.transport;

@SuppressWarnings("serial")
public final class InvalidCursorException extends IllegalArgumentException {
    public InvalidCursorException(String message) {
        super(message);
    }

    public InvalidCursorException(String message, Throwable cause) {
        super(message, cause);
    }
}

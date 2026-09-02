package com.cryptostrategy.platform.api.realtime;

final class RealtimeProtocolException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String code;
    private final boolean fatal;
    private final boolean retryable;

    RealtimeProtocolException(String code, String message, boolean fatal) {
        this(code, message, fatal, false);
    }

    RealtimeProtocolException(
            String code, String message, boolean fatal, boolean retryable) {
        super(message);
        this.code = code;
        this.fatal = fatal;
        this.retryable = retryable;
    }

    String code() {
        return code;
    }

    boolean fatal() {
        return fatal;
    }

    boolean retryable() {
        return retryable;
    }
}

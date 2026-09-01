package com.cryptostrategy.platform.news.api.error;

import java.util.Map;
import java.util.Objects;

/** Stable News error that carries only non-sensitive diagnostic context. */
@SuppressWarnings("serial")
public final class NewsException extends RuntimeException {
    private final NewsErrorCode code;
    private final Map<String, String> context;

    public NewsException(NewsErrorCode code, String message) {
        this(code, message, Map.of(), null);
    }

    public NewsException(NewsErrorCode code, String message, Map<String, String> context, Throwable cause) {
        super(Objects.requireNonNull(message, "message"), cause);
        this.code = Objects.requireNonNull(code, "code");
        this.context = Map.copyOf(Objects.requireNonNull(context, "context"));
    }

    public NewsErrorCode code() {
        return code;
    }

    public Map<String, String> context() {
        return context;
    }
}

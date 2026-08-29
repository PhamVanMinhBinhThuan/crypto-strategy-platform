package com.cryptostrategy.platform.marketdata.api.error;

import java.util.Map;
import java.util.Objects;

@SuppressWarnings("serial")
public final class MarketDataException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final MarketDataErrorCode code;
    private final Map<String, String> context;

    public MarketDataException(MarketDataErrorCode code, String message) { this(code, message, Map.of(), null); }
    public MarketDataException(MarketDataErrorCode code, String message, Map<String, String> context) { this(code, message, context, null); }
    public MarketDataException(MarketDataErrorCode code, String message, Map<String, String> context, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
        this.context = Map.copyOf(context);
    }
    public MarketDataErrorCode code() { return code; }
    public Map<String, String> context() { return context; }
}

package com.cryptostrategy.platform.strategy.api.error;

import java.io.Serial;

public final class StrategyException extends RuntimeException {
    @Serial private static final long serialVersionUID = 1L;
    private final StrategyErrorCode code;
    public StrategyException(StrategyErrorCode code, String message) { super(message); this.code = code; }
    public StrategyException(StrategyErrorCode code, String message, Throwable cause) { super(message, cause); this.code = code; }
    public StrategyErrorCode code() { return code; }
}

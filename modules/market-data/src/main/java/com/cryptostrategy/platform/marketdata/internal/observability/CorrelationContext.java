package com.cryptostrategy.platform.marketdata.internal.observability;

import java.util.Optional;
import org.slf4j.MDC;

public record CorrelationContext(Optional<String> correlationId) {
    public static final String KEY = "correlationId";
    public CorrelationContext { correlationId = correlationId == null ? Optional.empty() : correlationId; }
    public static CorrelationContext capture() { return new CorrelationContext(Optional.ofNullable(MDC.get(KEY))); }
    public Runnable wrap(Runnable action) {
        return () -> { String previous = MDC.get(KEY); try { correlationId.ifPresentOrElse(value -> MDC.put(KEY, value), () -> MDC.remove(KEY)); action.run(); }
            finally { if (previous == null) MDC.remove(KEY); else MDC.put(KEY, previous); } };
    }
}

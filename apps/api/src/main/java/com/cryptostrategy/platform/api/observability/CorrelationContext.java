package com.cryptostrategy.platform.api.observability;

import org.slf4j.MDC;

/** Scoped correlation context shared by servlet, asynchronous, and realtime boundaries. */
public final class CorrelationContext {
    private CorrelationContext() {
    }

    public static Scope open(String candidate) {
        String correlationId = CorrelationId.resolve(candidate);
        String previousValue = MDC.get(CorrelationId.MDC_KEY);
        MDC.put(CorrelationId.MDC_KEY, correlationId);
        return new Scope(correlationId, previousValue);
    }

    public static String current() {
        return MDC.get(CorrelationId.MDC_KEY);
    }

    public static final class Scope implements AutoCloseable {
        private final String correlationId;
        private final String previousValue;
        private boolean closed;

        private Scope(String correlationId, String previousValue) {
            this.correlationId = correlationId;
            this.previousValue = previousValue;
        }

        public String correlationId() {
            return correlationId;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previousValue == null) {
                MDC.remove(CorrelationId.MDC_KEY);
            } else {
                MDC.put(CorrelationId.MDC_KEY, previousValue);
            }
        }
    }
}

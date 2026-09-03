package com.cryptostrategy.platform.worker.search.coordination;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Metrics hữu hạn; log chỉ chứa identity/correlation đã chuẩn hóa, không chứa payload hoặc exception. */
public final class SearchObservability {
    private static final Logger log = LoggerFactory.getLogger(SearchObservability.class);
    private final MeterRegistry meters;

    public SearchObservability(MeterRegistry meters) {
        this.meters = Objects.requireNonNull(meters, "meters");
    }

    public void coordinationSucceeded(String experimentId, String correlationId) {
        meters.counter("search.coordination", "outcome", "success").increment();
        log.debug("Search coordination succeeded experimentId={} correlationId={}",
                safeId(experimentId), safeId(correlationId));
    }

    public void coordinationFailed(String experimentId, String correlationId, String failureCode) {
        meters.counter("search.coordination", "outcome", "failure", "code", safeCode(failureCode)).increment();
        log.warn("Search coordination failed experimentId={} correlationId={} code={}",
                safeId(experimentId), safeId(correlationId), safeCode(failureCode));
    }

    static String safeId(String value) {
        if (value == null || !value.matches("^[A-Za-z0-9:_-]{1,80}$")) return "redacted";
        return value;
    }

    static String safeCode(String value) {
        return value != null && value.matches("^[A-Z][A-Z0-9_]{0,63}$") ? value : "INTERNAL_ERROR";
    }
}

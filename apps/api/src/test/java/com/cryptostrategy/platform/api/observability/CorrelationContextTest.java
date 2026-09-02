package com.cryptostrategy.platform.api.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrelationContextTest {
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void nestedScopeRestoresTheCallingContext() {
        MDC.put(CorrelationId.MDC_KEY, "OUTER-CORRELATION-123");

        try (CorrelationContext.Scope scope = CorrelationContext.open("INNER-CORRELATION-456")) {
            assertThat(scope.correlationId()).isEqualTo("INNER-CORRELATION-456");
            assertThat(CorrelationContext.current()).isEqualTo("INNER-CORRELATION-456");
        }

        assertThat(CorrelationContext.current()).isEqualTo("OUTER-CORRELATION-123");
    }

    @Test
    void emptyCandidateCreatesAReusableUlidContext() {
        try (CorrelationContext.Scope scope = CorrelationContext.open(null)) {
            assertThat(scope.correlationId()).matches("[0-9A-HJKMNP-TV-Z]{26}");
            assertThat(CorrelationContext.current()).isEqualTo(scope.correlationId());
        }

        assertThat(CorrelationContext.current()).isNull();
    }
}

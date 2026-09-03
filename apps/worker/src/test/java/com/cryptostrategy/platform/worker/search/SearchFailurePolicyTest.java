package com.cryptostrategy.platform.worker.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.infra.redis.DeadLetterPublisher;
import com.cryptostrategy.platform.worker.infra.redis.LifecycleNotificationPublisher;
import com.cryptostrategy.platform.worker.search.coordination.SearchFailureHandler;
import com.cryptostrategy.platform.worker.search.coordination.SearchObservability;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class SearchFailurePolicyTest {
    private static final String EXPERIMENT = "01J7K8M9N0P1Q2R3S4T5A6V7W2";
    private static final String JOB = "01J7K8M9N0P1Q2R3S4T5A6V7W3";
    private static final String MESSAGE = "01J7K8M9N0P1Q2R3S4T5A6V7W4";

    @Test
    void retriesAreBoundedAndTerminalPublicationRedactsInternalFailureDetail() {
        DeadLetterPublisher deadLetters = mock(DeadLetterPublisher.class);
        LifecycleNotificationPublisher lifecycle = mock(LifecycleNotificationPublisher.class);
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        var handler = new SearchFailureHandler(
                new WorkerProperties(null, null, null, null,
                        new WorkerProperties.Retry(2, null, 2, null, 0), null, null, null),
                deadLetters, lifecycle, new SearchObservability(meters));
        var unsafe = new SearchFailureHandler.Failure(EXPERIMENT, JOB, null, MESSAGE,
                "correlation-f010", "SQLException password=/secret stack", 1);

        assertThat(handler.handle(unsafe)).isTrue();
        verify(deadLetters, never()).publishDeadLetter(
                eq(EXPERIMENT), eq(JOB), eq(null), eq(MESSAGE),
                eq("SEARCH_COORDINATION"), eq("INTERNAL_ERROR"), eq("search-failure:" + MESSAGE), eq(1));

        assertThat(handler.handle(new SearchFailureHandler.Failure(
                EXPERIMENT, JOB, null, MESSAGE, "correlation-f010", unsafe.failureCode(), 2))).isFalse();
        verify(deadLetters).publishDeadLetter(EXPERIMENT, JOB, null, MESSAGE,
                "SEARCH_COORDINATION", "INTERNAL_ERROR", "search-failure:" + MESSAGE, 2);
        verify(lifecycle).publishLifecycleNotification("EXPERIMENT", EXPERIMENT, EXPERIMENT,
                JOB, null, "SEARCH_FAILED", "correlation-f010");
        assertThat(meters.get("search.coordination").tag("outcome", "failure")
                .tag("code", "INTERNAL_ERROR").counter().count()).isEqualTo(2);
    }
}

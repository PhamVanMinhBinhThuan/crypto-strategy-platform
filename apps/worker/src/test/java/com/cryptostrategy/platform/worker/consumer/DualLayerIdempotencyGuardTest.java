package com.cryptostrategy.platform.worker.consumer;

import com.cryptostrategy.platform.persistence.api.worker.ProcessedMessageStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DualLayerIdempotencyGuardTest {

    private ProcessedMessageStore processedMessageStore;
    private DualLayerIdempotencyGuard guard;

    @BeforeEach
    void setUp() {
        processedMessageStore = mock(ProcessedMessageStore.class);
        guard = new DualLayerIdempotencyGuard(processedMessageStore);
    }

    @Test
    void isAlreadyProcessedDelegatesToStore() {
        when(processedMessageStore.isProcessed("consumer-1", "01J7K8M9N0P1Q2R3S4T5A6V7W1")).thenReturn(true);
        assertThat(guard.isAlreadyProcessed("consumer-1", "01J7K8M9N0P1Q2R3S4T5A6V7W1")).isTrue();
    }

    @Test
    void markProcessedInsertsWithRetention() {
        when(processedMessageStore.insertIfAbsent(eq("consumer-1"), eq("01J7K8M9N0P1Q2R3S4T5A6V7W1"), any(), any()))
                .thenReturn(true);

        boolean result = guard.markProcessed("consumer-1", "01J7K8M9N0P1Q2R3S4T5A6V7W1", Duration.ofDays(7));
        assertThat(result).isTrue();
        verify(processedMessageStore).insertIfAbsent(eq("consumer-1"), eq("01J7K8M9N0P1Q2R3S4T5A6V7W1"), any(), any());
    }
}

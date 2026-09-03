package com.cryptostrategy.platform.api.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class WorkEventBridgeTest {
    @Test
    void sameSubscriptionIdOnTwoConnectionsHasIndependentDeliveryAndCleanup() throws Exception {
        var bridge = new WorkEventBridge();
        var first = new ArrayList<RealtimeMessageMapper.ServerEvent>();
        var second = new ArrayList<RealtimeMessageMapper.ServerEvent>();
        var firstHandle = bridge.subscribe(WorkEventBridge.Kind.EXPERIMENT,
                "experiment", "corr", "progress", first::add);
        var secondHandle = bridge.subscribe(WorkEventBridge.Kind.EXPERIMENT,
                "experiment", "corr", "progress", second::add);

        bridge.publishLifecycle("experiment", "job", "RUNNING", "corr", Instant.EPOCH);
        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        firstHandle.close();
        bridge.publishLifecycle("experiment", "job", "COMPLETED", "corr", Instant.EPOCH);
        assertThat(first).hasSize(1);
        assertThat(second).hasSize(2);
        secondHandle.close();
        bridge.publishLifecycle("experiment", "job", "COMPLETED", "corr", Instant.EPOCH);
        assertThat(second).hasSize(2);
    }

    @Test
    void subscriberCanCloseDuringDeliveryWithoutInterruptingOtherConnections() {
        var bridge = new WorkEventBridge();
        var remaining = new ArrayList<RealtimeMessageMapper.ServerEvent>();
        AutoCloseable[] handle = new AutoCloseable[1];
        handle[0] = bridge.subscribe(WorkEventBridge.Kind.EXPERIMENT,
                "experiment", "corr", "closing", event -> {
                    try {
                        handle[0].close();
                    } catch (Exception exception) {
                        throw new AssertionError(exception);
                    }
                });
        bridge.subscribe(WorkEventBridge.Kind.EXPERIMENT,
                "experiment", "corr", "remaining", remaining::add);

        bridge.publishLifecycle("experiment", "job", "COMPLETED", "corr", Instant.EPOCH);
        assertThat(remaining).hasSize(1);
    }
}

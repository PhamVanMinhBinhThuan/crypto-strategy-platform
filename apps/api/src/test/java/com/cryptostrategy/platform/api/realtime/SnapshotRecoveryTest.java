package com.cryptostrategy.platform.api.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SnapshotRecoveryTest {
    @Test
    void buffersEventsUntilConfirmationBoundaryAndPreservesOrder() {
        var delivered = new ArrayList<RealtimeMessageMapper.ServerEvent>();
        var registry = new SubscriptionRegistry(4, 4);
        registry.reserve(
                "session",
                "experiment-1",
                SubscriptionRegistry.Type.EXPERIMENT,
                "01J00000000000000000000001",
                delivered::add);
        var first = event("progress-1");
        var second = event("progress-2");
        registry.publish("session", "experiment-1", first);
        registry.publish("session", "experiment-1", second);

        assertThat(delivered).isEmpty();
        List<RealtimeMessageMapper.ServerEvent> pending = registry.activate(
                "session", "experiment-1", () -> {});
        pending.forEach(delivered::add);

        assertThat(delivered).containsExactly(first, second);
    }

    @Test
    void reconnectActivationMarkersAreNeverReused() {
        var experiments = org.mockito.Mockito.mock(
                com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase.class);
        var leaderboards = org.mockito.Mockito.mock(
                com.cryptostrategy.platform.leaderboard.api.port.in.GetLeaderboardUseCase.class);
        var coordinator = new SnapshotCoordinator(experiments, leaderboards);

        assertThat(coordinator.candleMarker()).isNotEqualTo(coordinator.candleMarker());
    }

    private static RealtimeMessageMapper.ServerEvent event(String key) {
        return new RealtimeMessageMapper.ServerEvent(
                "EXPERIMENT_PROGRESS_UPDATED",
                Instant.EPOCH,
                "corr",
                "experiment-1",
                Map.of("sequence", key),
                true,
                key);
    }
}

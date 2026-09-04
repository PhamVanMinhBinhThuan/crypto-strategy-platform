package com.cryptostrategy.platform.api.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SnapshotRecoveryTest {
    @Test
    void lateCallbackCannotEnterReplacementSubscriptionWithTheSameClientId() {
        var delivered = new ArrayList<RealtimeMessageMapper.ServerEvent>();
        var registry = new SubscriptionRegistry(4, 4);
        var old = registry.reserve("session", "experiment-1", SubscriptionRegistry.Type.EXPERIMENT,
                "old-resource", delivered::add);
        registry.remove("session", "experiment-1", SubscriptionRegistry.Type.EXPERIMENT);
        var replacement = registry.reserve("session", "experiment-1", SubscriptionRegistry.Type.EXPERIMENT,
                "new-resource", delivered::add);
        registry.activate("session", "experiment-1", replacement, () -> {});
        registry.publish("session", "experiment-1", old, event("old"));
        registry.discard("session", "experiment-1", old);
        var current = event("current");
        registry.publish("session", "experiment-1", replacement, current);

        assertThat(delivered).containsExactly(current);
    }

    @Test
    void activationOverflowRequiresSnapshotRecoveryWithoutConfirmingOrAffectingOtherSubscriptions() {
        var registry = new SubscriptionRegistry(4, 4, 2);
        var delivered = new ArrayList<RealtimeMessageMapper.ServerEvent>();
        var overloaded = registry.reserve("session", "experiment-1", SubscriptionRegistry.Type.EXPERIMENT,
                "resource", delivered::add);
        for (int index = 0; index < 1000; index++) {
            registry.publish("session", "experiment-1", overloaded, event("progress-" + index));
        }

        assertThatThrownBy(() -> registry.activate("session", "experiment-1", overloaded,
                () -> { throw new AssertionError("Overflowed subscription must not be confirmed"); }))
                .isInstanceOf(RealtimeProtocolException.class)
                .extracting(exception -> ((RealtimeProtocolException) exception).retryable()).isEqualTo(true);
        var other = registry.reserve("session", "other", SubscriptionRegistry.Type.EXPERIMENT,
                "other-resource", delivered::add);
        registry.activate("session", "other", other, () -> {});
        var current = event("current");
        registry.publish("session", "other", other, current);
        assertThat(delivered).containsExactly(current);
    }

    @Test
    void buffersEventsUntilConfirmationBoundaryAndPreservesOrder() {
        var delivered = new ArrayList<RealtimeMessageMapper.ServerEvent>();
        var registry = new SubscriptionRegistry(4, 4);
        var registration = registry.reserve(
                "session",
                "experiment-1",
                SubscriptionRegistry.Type.EXPERIMENT,
                "01J00000000000000000000001",
                delivered::add);
        var first = event("progress-1");
        var second = event("progress-2");
        registry.publish("session", "experiment-1", registration, first);
        registry.publish("session", "experiment-1", registration, second);

        assertThat(delivered).isEmpty();
        registry.activate("session", "experiment-1", registration, () -> assertThat(delivered).isEmpty());

        assertThat(delivered).containsExactly(first, second);
    }

    @Test
    void reconnectActivationMarkersAreNeverReused() {
        var experiments = org.mockito.Mockito.mock(
                com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase.class);
        var leaderboards = org.mockito.Mockito.mock(
                com.cryptostrategy.platform.leaderboard.api.port.in.GetLeaderboardUseCase.class);
        var coordinator = new SnapshotCoordinator(experiments, leaderboards);

        Map<String, Object> first = coordinator.candleRecovery("BTC/USDT", "5m");
        Map<String, Object> second = coordinator.candleRecovery("BTC/USDT", "5m");

        assertThat(first.get("syncMarker")).isNotEqualTo(second.get("syncMarker"));
        assertThat(first)
                .containsEntry("snapshotUrl", "/api/v1/candles")
                .containsEntry("freshness", "STALE_UNTIL_RECONCILED")
                .containsEntry("reconciliation", "REST_BACKFILL_THEN_REALTIME");
        assertThat(first.get("snapshotQuery"))
                .isEqualTo(Map.of("pair", "BTC/USDT", "timeframe", "5m"));
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

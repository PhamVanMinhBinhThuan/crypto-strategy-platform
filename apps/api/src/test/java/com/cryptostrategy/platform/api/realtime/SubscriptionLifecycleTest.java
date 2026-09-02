package com.cryptostrategy.platform.api.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.leaderboard.api.port.in.GetLeaderboardUseCase;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionLifecycleTest {
    @Test
    void enforcesDuplicateAndFamilyLimitsWithoutAffectingOtherSubscriptions() {
        var registry = new SubscriptionRegistry(4, 4);
        for (int index = 0; index < 4; index++) {
            registry.reserve("session", "chart-" + index,
                    SubscriptionRegistry.Type.CANDLES, "BTC/USDT|5m", ignored -> {});
        }

        assertThatThrownBy(() -> registry.reserve(
                        "session", "chart-4", SubscriptionRegistry.Type.CANDLES,
                        "ETH/USDT|5m", ignored -> {}))
                .isInstanceOf(RealtimeProtocolException.class)
                .extracting(exception -> ((RealtimeProtocolException) exception).code())
                .isEqualTo("MARKET_SUBSCRIPTION_LIMIT_EXCEEDED");
        assertThatThrownBy(() -> registry.reserve(
                        "session", "chart-0", SubscriptionRegistry.Type.EXPERIMENT,
                        "experiment", ignored -> {}))
                .isInstanceOf(RealtimeProtocolException.class)
                .extracting(exception -> ((RealtimeProtocolException) exception).code())
                .isEqualTo("DUPLICATE_SUBSCRIPTION_ID");

        registry.remove("session", "chart-0", SubscriptionRegistry.Type.CANDLES);
        registry.reserve("session", "chart-4", SubscriptionRegistry.Type.CANDLES,
                "ETH/USDT|5m", ignored -> {});
    }

    @Test
    void concealsForeignExperimentAndLeaderboardSubscriptions() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID foreign = UUID.fromString("00000000-0000-0000-0000-000000000002");
        ExperimentId id = new ExperimentId("01J00000000000000000000001");
        GetExperimentUseCase experiments = mock(GetExperimentUseCase.class);
        GetLeaderboardUseCase leaderboards = mock(GetLeaderboardUseCase.class);
        when(experiments.getExperiment(owner, id)).thenReturn(Optional.of(Experiment.create(
                id, owner, "owned", null, null, Instant.EPOCH)));
        var coordinator = new SnapshotCoordinator(experiments, leaderboards);

        assertThat(coordinator.authorizeExperiment(owner, id.value()).get("snapshotUrl"))
                .isEqualTo("/api/v1/experiments/" + id.value());
        assertThatThrownBy(() -> coordinator.authorizeExperiment(foreign, id.value()))
                .isInstanceOf(RealtimeProtocolException.class)
                .hasMessage("The requested resource was not found");
        assertThatThrownBy(() -> coordinator.authorizeLeaderboard(foreign, id.value()))
                .isInstanceOf(RealtimeProtocolException.class)
                .hasMessage("The requested resource was not found");
    }
}

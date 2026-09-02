package com.cryptostrategy.platform.api.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RealtimeConnectionTest {
    private static final Instant CONNECTED_AT = Instant.parse("2026-09-02T00:00:00Z");

    @Test
    void authenticationExpiryWinsWhenItComesFirst() {
        Instant authenticationExpiry = CONNECTED_AT.plus(Duration.ofMinutes(5));

        assertThat(RealtimeConnection.deadline(
                        CONNECTED_AT, authenticationExpiry, Duration.ofMinutes(30)))
                .isEqualTo(authenticationExpiry);
    }

    @Test
    void maximumConnectionLifetimeWinsWhenAuthenticationLastsLonger() {
        assertThat(RealtimeConnection.deadline(
                        CONNECTED_AT,
                        CONNECTED_AT.plus(Duration.ofHours(2)),
                        Duration.ofMinutes(30)))
                .isEqualTo(CONNECTED_AT.plus(Duration.ofMinutes(30)));
    }
}

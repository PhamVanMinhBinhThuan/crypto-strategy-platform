package com.cryptostrategy.platform.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class WebSocketTicketServiceTest {
    private static final UUID USER = UUID.fromString("9b0f36b1-6004-49aa-a6d1-1cc2f373741f");
    private static final String ORIGIN = "https://dashboard.example.test";

    @Test
    void ticketIsBoundToOriginAndCanBeConsumedOnlyOnce() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-02T00:00:00Z"), ZoneOffset.UTC);
        WebSocketTicketService service = new WebSocketTicketService(Duration.ofSeconds(60), clock, new SecureRandom());

        var issued = service.issue(USER, ORIGIN);
        assertThat(service.consume(issued.ticket(), ORIGIN).userId()).isEqualTo(USER);
        assertThatThrownBy(() -> service.consume(issued.ticket(), ORIGIN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void wrongOriginAndExpiredTicketsAreRejected() {
        Instant now = Instant.parse("2026-09-02T00:00:00Z");
        MutableClock clock = new MutableClock(now);
        WebSocketTicketService service = new WebSocketTicketService(Duration.ofSeconds(60), clock, new SecureRandom());
        var issued = service.issue(USER, ORIGIN);

        assertThatThrownBy(() -> service.consume(issued.ticket(), "https://evil.example.test"))
                .isInstanceOf(IllegalArgumentException.class);

        clock.advance(Duration.ofSeconds(60));
        assertThatThrownBy(() -> service.consume(issued.ticket(), ORIGIN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;
        private MutableClock(Instant initial) { current = new AtomicReference<>(initial); }
        private void advance(Duration amount) { current.updateAndGet(value -> value.plus(amount)); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return current.get(); }
    }
}

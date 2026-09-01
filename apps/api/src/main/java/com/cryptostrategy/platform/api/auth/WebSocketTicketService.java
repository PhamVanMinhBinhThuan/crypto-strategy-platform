package com.cryptostrategy.platform.api.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Issues short-lived, single-use credentials for browser WebSocket handshakes. */
@Service
public final class WebSocketTicketService {
    private final ConcurrentMap<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final SecureRandom random;
    private final Clock clock;
    private final Duration lifetime;

    @Autowired
    public WebSocketTicketService(
            @Value("${platform.security.websocket-ticket-lifetime:PT60S}") Duration lifetime) {
        this(lifetime, Clock.systemUTC(), new SecureRandom());
    }

    WebSocketTicketService(Duration lifetime, Clock clock, SecureRandom random) {
        if (lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("WebSocket ticket lifetime must be positive");
        }
        this.lifetime = lifetime;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.random = Objects.requireNonNull(random, "random");
    }

    public IssuedTicket issue(UUID userId, String origin) {
        Objects.requireNonNull(userId, "userId");
        String normalizedOrigin = requireOrigin(origin);
        Instant expiresAt = clock.instant().plus(lifetime);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(token, new Ticket(userId, normalizedOrigin, expiresAt));
        return new IssuedTicket(token, expiresAt);
    }

    public AuthenticatedUserContext consume(String token, String origin) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("WebSocket ticket is required");
        }
        String normalizedOrigin = requireOrigin(origin);
        Ticket ticket = tickets.get(token);
        if (ticket == null || clock.instant().compareTo(ticket.expiresAt()) >= 0
                || !ticket.origin().equals(normalizedOrigin)) {
            throw new IllegalArgumentException("WebSocket ticket is invalid or expired");
        }
        if (!tickets.remove(token, ticket)) {
            throw new IllegalArgumentException("WebSocket ticket has already been consumed");
        }
        return new AuthenticatedUserContext(ticket.userId());
    }

    private static String requireOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("WebSocket origin is required");
        }
        return origin.trim();
    }

    private record Ticket(UUID userId, String origin, Instant expiresAt) {}

    public record IssuedTicket(String ticket, Instant expiresAt) {
        public IssuedTicket {
            Objects.requireNonNull(ticket, "ticket");
            Objects.requireNonNull(expiresAt, "expiresAt");
        }
    }
}

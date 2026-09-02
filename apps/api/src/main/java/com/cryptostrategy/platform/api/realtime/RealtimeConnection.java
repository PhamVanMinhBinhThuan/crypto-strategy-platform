package com.cryptostrategy.platform.api.realtime;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** Owns authenticated WebSocket connection expiry and cleanup. */
@Component
public final class RealtimeConnection extends TextWebSocketHandler {
    public static final CloseStatus REAUTHENTICATION_REQUIRED =
            new CloseStatus(4001, "REAUTHENTICATION_REQUIRED");

    private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeConnection.class);

    private final Duration maximumLifetime;
    private final Clock clock;
    private final TaskScheduler scheduler;
    private final ConcurrentMap<String, ScheduledFuture<?>> expiryTasks =
            new ConcurrentHashMap<>();

    public RealtimeConnection(
            @Value("${platform.security.websocket-max-connection-lifetime:PT30M}")
                    Duration maximumLifetime,
            @Qualifier("realtimeClock") Clock clock,
            @Qualifier("realtimeTaskScheduler") TaskScheduler scheduler) {
        if (maximumLifetime.isZero() || maximumLifetime.isNegative()) {
            throw new IllegalArgumentException(
                    "WebSocket maximum connection lifetime must be positive");
        }
        this.maximumLifetime = maximumLifetime;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        AuthenticatedUserContext user = authenticatedUser(session);
        Instant connectedAt = clock.instant();
        Instant deadline = deadline(
                connectedAt, user.authenticationExpiresAt(), maximumLifetime);
        ScheduledFuture<?> task = scheduler.schedule(() -> expire(session), deadline);
        if (task == null) {
            throw new IllegalStateException("Unable to schedule WebSocket authentication expiry");
        }
        ScheduledFuture<?> replaced = expiryTasks.put(session.getId(), task);
        if (replaced != null) {
            replaced.cancel(false);
            task.cancel(false);
            throw new IllegalStateException("WebSocket session is already active");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cancelExpiry(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
            throws Exception {
        cancelExpiry(session.getId());
        super.handleTransportError(session, exception);
    }

    static Instant deadline(
            Instant connectedAt, Instant authenticationExpiresAt, Duration maximumLifetime) {
        Instant maximumDeadline = connectedAt.plus(maximumLifetime);
        return authenticationExpiresAt.compareTo(maximumDeadline) <= 0
                ? authenticationExpiresAt
                : maximumDeadline;
    }

    private static AuthenticatedUserContext authenticatedUser(WebSocketSession session) {
        if (session.getPrincipal()
                instanceof WebSocketTicketHandshakeHandler.RealtimePrincipal principal) {
            return principal.user();
        }
        throw new IllegalStateException("Authenticated WebSocket principal is missing");
    }

    private void expire(WebSocketSession session) {
        expiryTasks.remove(session.getId());
        if (!session.isOpen()) {
            return;
        }
        try {
            session.close(REAUTHENTICATION_REQUIRED);
        } catch (IOException exception) {
            LOGGER.warn("Unable to close expired WebSocket session sessionId={}",
                    session.getId());
        }
    }

    private void cancelExpiry(String sessionId) {
        ScheduledFuture<?> task = expiryTasks.remove(sessionId);
        if (task != null) {
            task.cancel(false);
        }
    }
}

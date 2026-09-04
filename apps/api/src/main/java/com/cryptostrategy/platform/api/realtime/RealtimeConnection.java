package com.cryptostrategy.platform.api.realtime;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** Authenticated multiplexed realtime connection with bounded protocol controls. */
@Component
public final class RealtimeConnection extends TextWebSocketHandler {
    public static final CloseStatus REAUTHENTICATION_REQUIRED =
            new CloseStatus(4001, "REAUTHENTICATION_REQUIRED");
    static final CloseStatus HEARTBEAT_TIMEOUT = new CloseStatus(4002, "HEARTBEAT_TIMEOUT");
    static final CloseStatus RATE_LIMITED = new CloseStatus(4008, "RATE_LIMIT_EXCEEDED");

    private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeConnection.class);

    private final Duration maximumLifetime;
    private final Duration commandWindow;
    private final Duration heartbeatTimeout;
    private final int maximumCommands;
    private final int maximumMessageBytes;
    private final Clock clock;
    private final TaskScheduler scheduler;
    private final RealtimeMessageMapper messages;
    private final SubscriptionRegistry subscriptions;
    private final SnapshotCoordinator snapshots;
    private final MarketEventBridge market;
    private final WorkEventBridge work;
    private final RealtimeDeliveryService delivery;
    private final ConcurrentMap<String, SessionState> sessions = new ConcurrentHashMap<>();

    public RealtimeConnection(
            @Value("${platform.security.websocket-max-connection-lifetime:PT30M}")
                    Duration maximumLifetime,
            @Value("${platform.realtime.command-window:PT10S}") Duration commandWindow,
            @Value("${platform.realtime.heartbeat-timeout:PT90S}") Duration heartbeatTimeout,
            @Value("${platform.realtime.max-commands-per-window:30}") int maximumCommands,
            @Value("${platform.realtime.max-message-bytes:65536}") int maximumMessageBytes,
            @Qualifier("realtimeClock") Clock clock,
            @Qualifier("realtimeTaskScheduler") TaskScheduler scheduler,
            RealtimeMessageMapper messages,
            SubscriptionRegistry subscriptions,
            SnapshotCoordinator snapshots,
            MarketEventBridge market,
            WorkEventBridge work,
            RealtimeDeliveryService delivery) {
        if (maximumLifetime.isZero() || maximumLifetime.isNegative()
                || commandWindow.isZero() || commandWindow.isNegative()
                || heartbeatTimeout.isZero() || heartbeatTimeout.isNegative()
                || maximumCommands < 1 || maximumMessageBytes < 1024) {
            throw new IllegalArgumentException("Realtime connection limits are invalid");
        }
        this.maximumLifetime = maximumLifetime;
        this.commandWindow = commandWindow;
        this.heartbeatTimeout = heartbeatTimeout;
        this.maximumCommands = maximumCommands;
        this.maximumMessageBytes = maximumMessageBytes;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.subscriptions = Objects.requireNonNull(subscriptions, "subscriptions");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
        this.market = Objects.requireNonNull(market, "market");
        this.work = Objects.requireNonNull(work, "work");
        this.delivery = Objects.requireNonNull(delivery, "delivery");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        AuthenticatedUserContext user = authenticatedUser(session);
        Instant connectedAt = clock.instant();
        SessionState state = new SessionState(user, connectedAt);
        if (sessions.putIfAbsent(session.getId(), state) != null) {
            throw new IllegalStateException("WebSocket session is already active");
        }
        delivery.open(session);
        state.expiryTask = requireTask(scheduler.schedule(
                () -> closeSession(session, REAUTHENTICATION_REQUIRED),
                deadline(connectedAt, user.authenticationExpiresAt(), maximumLifetime)));
        scheduleHeartbeat(session, state, connectedAt.plus(heartbeatTimeout));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        SessionState state = sessions.get(session.getId());
        if (state == null) {
            closeSession(session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        if (message.asBytes().length > maximumMessageBytes) {
            closeSession(session, CloseStatus.TOO_BIG_TO_PROCESS);
            return;
        }
        Instant now = clock.instant();
        if (!state.acceptCommand(now, commandWindow, maximumCommands)) {
            closeSession(session, RATE_LIMITED);
            return;
        }
        RealtimeMessageMapper.ClientCommand command = null;
        try {
            command = messages.read(message.getPayload());
            apply(session, state, command);
        } catch (RealtimeProtocolException exception) {
            if (exception.fatal()) {
                closeSession(session, CloseStatus.POLICY_VIOLATION);
            } else {
                sendError(session, command, exception);
            }
        } catch (MarketDataException exception) {
            sendError(session, command, marketError(exception));
        } catch (RuntimeException exception) {
            sendError(session, command, new RealtimeProtocolException(
                    "SUBSCRIPTION_FAILED", "The subscription could not be applied", false));
        }
    }

    private static RealtimeProtocolException marketError(MarketDataException exception) {
        String code = switch (exception.code()) {
            case INVALID_MARKET_QUERY -> "INVALID_MARKET_QUERY";
            case MARKET_PROVIDER_RATE_LIMITED -> "MARKET_PROVIDER_RATE_LIMITED";
            default -> "MARKET_PROVIDER_UNAVAILABLE";
        };
        return new RealtimeProtocolException(
                code,
                "The market subscription is temporarily unavailable",
                false,
                !"INVALID_MARKET_QUERY".equals(code));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cleanup(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
            throws Exception {
        cleanup(session.getId());
        super.handleTransportError(session, exception);
    }

    static Instant deadline(
            Instant connectedAt, Instant authenticationExpiresAt, Duration maximumLifetime) {
        Instant maximumDeadline = connectedAt.plus(maximumLifetime);
        return authenticationExpiresAt.compareTo(maximumDeadline) <= 0
                ? authenticationExpiresAt
                : maximumDeadline;
    }

    private void apply(
            WebSocketSession session,
            SessionState state,
            RealtimeMessageMapper.ClientCommand command) {
        switch (command.eventType()) {
            case "SUBSCRIBE_CANDLES" -> subscribeCandles(session, command);
            case "UNSUBSCRIBE_CANDLES" -> unsubscribe(
                    session, command, SubscriptionRegistry.Type.CANDLES);
            case "SUBSCRIBE_EXPERIMENT" -> subscribeWork(
                    session, state, command, SubscriptionRegistry.Type.EXPERIMENT);
            case "UNSUBSCRIBE_EXPERIMENT" -> unsubscribe(
                    session, command, SubscriptionRegistry.Type.EXPERIMENT);
            case "SUBSCRIBE_LEADERBOARD" -> subscribeWork(
                    session, state, command, SubscriptionRegistry.Type.LEADERBOARD);
            case "UNSUBSCRIBE_LEADERBOARD" -> unsubscribe(
                    session, command, SubscriptionRegistry.Type.LEADERBOARD);
            case "PING" -> ping(session, command);
            default -> throw new RealtimeProtocolException(
                    "REQUEST_VALIDATION_FAILED", "Unsupported realtime command", false);
        }
    }

    private void subscribeCandles(
            WebSocketSession session, RealtimeMessageMapper.ClientCommand command) {
        requireOnly(command.payload(), Set.of("pair", "timeframe"));
        String pair = requiredPayloadText(command.payload(), "pair");
        String timeframe = requiredPayloadText(command.payload(), "timeframe");
        var registration = subscriptions.reserve(
                session.getId(),
                command.subscriptionId(),
                SubscriptionRegistry.Type.CANDLES,
                pair + "|" + timeframe,
                event -> delivery.send(session, event));
        try {
            AutoCloseable handle = market.subscribe(
                    pair,
                    timeframe,
                    command.correlationId(),
                    command.subscriptionId(),
                    event -> subscriptions.publish(
                            session.getId(), command.subscriptionId(), registration, event));
            subscriptions.attach(session.getId(), command.subscriptionId(), registration, handle);
            subscriptions.activate(session.getId(), command.subscriptionId(), registration,
                    () -> confirm(
                            session,
                            command,
                            "CANDLES",
                            "ACTIVE",
                            snapshots.candleRecovery(pair, timeframe)));
        } catch (RuntimeException exception) {
            subscriptions.discard(
                    session.getId(), command.subscriptionId(), registration);
            throw exception;
        }
    }

    private void subscribeWork(
            WebSocketSession session,
            SessionState state,
            RealtimeMessageMapper.ClientCommand command,
            SubscriptionRegistry.Type type) {
        requireOnly(command.payload(), Set.of("experimentId"));
        String experimentId = requiredPayloadText(command.payload(), "experimentId");
        var registration = subscriptions.reserve(
                session.getId(),
                command.subscriptionId(),
                type,
                experimentId,
                event -> delivery.send(session, event));
        try {
            WorkEventBridge.Kind kind = type == SubscriptionRegistry.Type.EXPERIMENT
                    ? WorkEventBridge.Kind.EXPERIMENT
                    : WorkEventBridge.Kind.LEADERBOARD;
            AutoCloseable handle = work.subscribe(
                    kind,
                    experimentId,
                    command.correlationId(),
                    command.subscriptionId(),
                    event -> subscriptions.publish(
                            session.getId(), command.subscriptionId(), registration, event));
            subscriptions.attach(session.getId(), command.subscriptionId(), registration, handle);
            Map<String, Object> recovery = type == SubscriptionRegistry.Type.EXPERIMENT
                    ? snapshots.authorizeExperiment(state.user.userId(), experimentId)
                    : snapshots.authorizeLeaderboard(state.user.userId(), experimentId);
            subscriptions.activate(session.getId(), command.subscriptionId(), registration,
                    () -> confirm(session, command, type.name(), "ACTIVE", recovery));
        } catch (RuntimeException exception) {
            subscriptions.discard(session.getId(), command.subscriptionId(), registration);
            throw exception;
        }
    }

    private void unsubscribe(
            WebSocketSession session,
            RealtimeMessageMapper.ClientCommand command,
            SubscriptionRegistry.Type type) {
        requireOnly(command.payload(), Set.of());
        subscriptions.remove(session.getId(), command.subscriptionId(), type);
        confirm(session, command, type.name(), "INACTIVE", Map.of());
    }

    private void ping(
            WebSocketSession session, RealtimeMessageMapper.ClientCommand command) {
        requireOnly(command.payload(), Set.of("clientTime"));
        String clientTime = requiredPayloadText(command.payload(), "clientTime");
        delivery.send(session, RealtimeMessageMapper.event(
                "PONG",
                command.correlationId(),
                command.subscriptionId(),
                Map.of("clientTime", clientTime, "serverTime", clock.instant()),
                false,
                null));
    }

    private void confirm(
            WebSocketSession session,
            RealtimeMessageMapper.ClientCommand command,
            String type,
            String status,
            Map<String, Object> extra) {
        Map<String, Object> payload = new HashMap<>(extra);
        payload.put("subscriptionType", type);
        payload.put("status", status);
        delivery.send(session, RealtimeMessageMapper.event(
                "SUBSCRIPTION_CONFIRMED",
                command.correlationId(),
                command.subscriptionId(),
                payload,
                false,
                null));
    }

    private void sendError(
            WebSocketSession session,
            RealtimeMessageMapper.ClientCommand command,
            RealtimeProtocolException exception) {
        String correlationId = command == null ? "realtime" : command.correlationId();
        String subscriptionId = command == null ? "connection" : command.subscriptionId();
        delivery.send(session, RealtimeMessageMapper.event(
                "SUBSCRIPTION_ERROR",
                correlationId,
                subscriptionId,
                Map.of(
                        "code", exception.code(),
                        "message", exception.getMessage(),
                        "details", Map.of(),
                        "retryable", exception.retryable()),
                false,
                null));
    }

    private void scheduleHeartbeat(
            WebSocketSession session, SessionState state, Instant deadline) {
        state.heartbeatTask = requireTask(scheduler.schedule(() -> {
            SessionState current = sessions.get(session.getId());
            if (current == null) {
                return;
            }
            Instant now = clock.instant();
            Instant next = current.lastActivity.plus(heartbeatTimeout);
            if (!now.isBefore(next)) {
                closeSession(session, HEARTBEAT_TIMEOUT);
            } else {
                scheduleHeartbeat(session, current, next);
            }
        }, deadline));
    }

    private void closeSession(WebSocketSession session, CloseStatus status) {
        cleanup(session.getId());
        if (!session.isOpen()) {
            return;
        }
        try {
            session.close(status);
        } catch (IOException exception) {
            LOGGER.warn("Unable to close realtime session sessionId={}", session.getId());
        }
    }

    private void cleanup(String sessionId) {
        SessionState state = sessions.remove(sessionId);
        if (state != null) {
            cancel(state.expiryTask);
            cancel(state.heartbeatTask);
        }
        subscriptions.closeSession(sessionId);
        delivery.close(sessionId);
    }

    private static ScheduledFuture<?> requireTask(ScheduledFuture<?> task) {
        if (task == null) {
            throw new IllegalStateException("Unable to schedule realtime lifecycle task");
        }
        return task;
    }

    private static void cancel(ScheduledFuture<?> task) {
        if (task != null) {
            task.cancel(false);
        }
    }

    private static AuthenticatedUserContext authenticatedUser(WebSocketSession session) {
        if (session.getPrincipal()
                instanceof WebSocketTicketHandshakeHandler.RealtimePrincipal principal) {
            return principal.user();
        }
        throw new IllegalStateException("Authenticated WebSocket principal is missing");
    }

    private static String requiredPayloadText(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new RealtimeProtocolException(
                    "REQUEST_VALIDATION_FAILED", "Invalid command payload", false);
        }
        return value.textValue();
    }

    private static void requireOnly(JsonNode payload, Set<String> expected) {
        Set<String> actual = new HashSet<>();
        payload.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(expected)) {
            throw new RealtimeProtocolException(
                    "REQUEST_VALIDATION_FAILED", "Invalid command payload", false);
        }
    }

    private static final class SessionState {
        private final AuthenticatedUserContext user;
        private final ArrayDeque<Instant> commandTimes = new ArrayDeque<>();
        private volatile Instant lastActivity;
        private volatile ScheduledFuture<?> expiryTask;
        private volatile ScheduledFuture<?> heartbeatTask;

        private SessionState(AuthenticatedUserContext user, Instant connectedAt) {
            this.user = user;
            this.lastActivity = connectedAt;
        }

        private synchronized boolean acceptCommand(
                Instant now, Duration window, int maximumCommands) {
            Instant cutoff = now.minus(window);
            while (!commandTimes.isEmpty() && commandTimes.peekFirst().isBefore(cutoff)) {
                commandTimes.removeFirst();
            }
            if (commandTimes.size() >= maximumCommands) {
                return false;
            }
            commandTimes.addLast(now);
            lastActivity = now;
            return true;
        }
    }
}

package com.cryptostrategy.platform.api.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class RealtimeConnectionTest {
    private static final Instant CONNECTED_AT = Instant.parse("2026-09-02T00:00:00Z");

    @Test
    void failedAuthorizationClosesRegisteredListenerAndDoesNotConfirmSubscription() throws Exception {
        var scheduler = mock(TaskScheduler.class);
        when(scheduler.schedule(any(Runnable.class), any(Instant.class)))
                .thenAnswer(ignored -> mock(ScheduledFuture.class));
        var snapshots = mock(SnapshotCoordinator.class);
        var work = mock(WorkEventBridge.class);
        var delivery = mock(RealtimeDeliveryService.class);
        var handle = mock(AutoCloseable.class);
        when(work.subscribe(any(), anyString(), anyString(), anyString(), any())).thenReturn(handle);
        when(snapshots.authorizeExperiment(any(), anyString())).thenThrow(
                new RealtimeProtocolException("EXPERIMENT_NOT_FOUND", "The requested resource was not found", false));
        var connection = new RealtimeConnection(Duration.ofMinutes(30), Duration.ofSeconds(10),
                Duration.ofSeconds(90), 30, 65536, Clock.fixed(CONNECTED_AT, ZoneOffset.UTC),
                scheduler, new RealtimeMessageMapper(new ObjectMapper()), new SubscriptionRegistry(4, 4),
                snapshots, mock(MarketEventBridge.class), work, delivery);
        var session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session");
        when(session.isOpen()).thenReturn(true);
        when(session.getPrincipal()).thenReturn(new WebSocketTicketHandshakeHandler.RealtimePrincipal(
                new AuthenticatedUserContext(UUID.randomUUID(), CONNECTED_AT.plusSeconds(300))));
        connection.afterConnectionEstablished(session);

        connection.handleTextMessage(session, new TextMessage("""
                {"eventType":"SUBSCRIBE_EXPERIMENT","eventVersion":1,"eventId":"request",
                 "occurredAt":"2026-09-02T00:00:00Z","correlationId":"corr","subscriptionId":"progress",
                 "payload":{"experimentId":"01J00000000000000000000001"}}
                """));

        verify(handle).close();
        var sent = org.mockito.ArgumentCaptor.forClass(RealtimeMessageMapper.ServerEvent.class);
        verify(delivery).send(eq(session), sent.capture());
        assertThat(sent.getValue().eventType()).isEqualTo("SUBSCRIPTION_ERROR");
        assertThat(sent.getValue().payload().get("code")).isEqualTo("EXPERIMENT_NOT_FOUND");
        verify(session, never()).close(any());
    }

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

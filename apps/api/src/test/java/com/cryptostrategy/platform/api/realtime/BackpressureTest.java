package com.cryptostrategy.platform.api.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

class BackpressureTest {
    @Test
    void coalescingDoesNotReplaceAnotherLogicalSubscriptionsUpdate() throws Exception {
        List<Runnable> scheduled = new ArrayList<>();
        WebSocketSession session = session("independent-subscriptions");
        var delivery = new RealtimeDeliveryService(
                new RealtimeMessageMapper(new ObjectMapper().findAndRegisterModules()), scheduler(scheduled), 8);
        delivery.open(session);
        delivery.send(session, new RealtimeMessageMapper.ServerEvent(
                "LEADERBOARD_UPDATED", Instant.EPOCH, "corr", "first", Map.of("revision", 1), true, "same-resource"));
        delivery.send(session, new RealtimeMessageMapper.ServerEvent(
                "LEADERBOARD_UPDATED", Instant.EPOCH, "corr", "second", Map.of("revision", 1), true, "same-resource"));
        for (int index = 0; index < 6; index++) {
            delivery.send(session, event("BACKTEST_COMPLETED", false, null));
        }
        delivery.send(session, new RealtimeMessageMapper.ServerEvent(
                "LEADERBOARD_UPDATED", Instant.EPOCH, "corr", "second", Map.of("revision", 2), true, "same-resource"));
        scheduled.getFirst().run();

        var sent = org.mockito.ArgumentCaptor.forClass(org.springframework.web.socket.TextMessage.class);
        verify(session, org.mockito.Mockito.times(8)).sendMessage(sent.capture());
        var json = new ObjectMapper();
        var revisions = new java.util.HashMap<String, Integer>();
        for (var message : sent.getAllValues()) {
            var node = json.readTree(message.getPayload());
            if (node.path("eventType").asText().equals("LEADERBOARD_UPDATED")) {
                revisions.put(node.path("subscriptionId").asText(), node.path("payload").path("revision").asInt());
            }
        }
        assertThat(revisions).containsExactlyInAnyOrderEntriesOf(Map.of("first", 1, "second", 2));
    }

    @Test
    void retainsTerminalEventByEvictingAnIntermediateUpdate() throws Exception {
        List<Runnable> scheduled = new ArrayList<>();
        TaskScheduler scheduler = scheduler(scheduled);
        WebSocketSession session = session("bounded");
        var delivery = new RealtimeDeliveryService(
                new RealtimeMessageMapper(new ObjectMapper().findAndRegisterModules()),
                scheduler,
                8);
        delivery.open(session);
        for (int index = 0; index < 8; index++) {
            delivery.send(session, event("PROGRESS", true, "progress-" + index));
        }
        delivery.send(session, event("BACKTEST_COMPLETED", false, null));

        assertThat(scheduled).hasSize(1);
        scheduled.getFirst().run();
        verify(session, org.mockito.Mockito.times(8)).sendMessage(any());
    }

    @Test
    void disconnectsInsteadOfDroppingWhenOnlyNonReplaceableEventsRemain() throws Exception {
        List<Runnable> scheduled = new ArrayList<>();
        WebSocketSession session = session("slow");
        var delivery = new RealtimeDeliveryService(
                new RealtimeMessageMapper(new ObjectMapper().findAndRegisterModules()),
                scheduler(scheduled),
                8);
        delivery.open(session);
        for (int index = 0; index < 9; index++) {
            delivery.send(session, event("BACKTEST_COMPLETED", false, null));
        }

        verify(session).close(RealtimeDeliveryService.SLOW_CONSUMER);
    }

    private static TaskScheduler scheduler(List<Runnable> scheduled) {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        when(scheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(invocation -> {
            scheduled.add(invocation.getArgument(0));
            return mock(java.util.concurrent.ScheduledFuture.class);
        });
        return scheduler;
    }

    private static WebSocketSession session(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private static RealtimeMessageMapper.ServerEvent event(
            String type, boolean coalescible, String key) {
        return new RealtimeMessageMapper.ServerEvent(
                type,
                Instant.EPOCH,
                "corr",
                "subscription",
                Map.of("status", type),
                coalescible,
                key);
    }
}

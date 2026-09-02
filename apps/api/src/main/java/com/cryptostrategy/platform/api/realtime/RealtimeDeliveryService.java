package com.cryptostrategy.platform.api.realtime;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/** Bounded per-connection queue with coalescing for replaceable intermediate events. */
@Component
public final class RealtimeDeliveryService {
    static final CloseStatus SLOW_CONSUMER = new CloseStatus(4008, "SLOW_CONSUMER");

    private final RealtimeMessageMapper mapper;
    private final TaskScheduler scheduler;
    private final int capacity;
    private final Map<String, State> states = new HashMap<>();

    public RealtimeDeliveryService(
            RealtimeMessageMapper mapper,
            @Qualifier("realtimeTaskScheduler") TaskScheduler scheduler,
            @Value("${platform.realtime.outbound-buffer-capacity:128}") int capacity) {
        if (capacity < 8) {
            throw new IllegalArgumentException("Realtime outbound capacity must be at least 8");
        }
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.capacity = capacity;
    }

    void open(WebSocketSession session) {
        synchronized (states) {
            states.put(session.getId(), new State(session));
        }
    }

    void send(WebSocketSession session, RealtimeMessageMapper.ServerEvent event) {
        State state;
        synchronized (states) {
            state = states.get(session.getId());
        }
        if (state == null) {
            return;
        }
        boolean scheduleDrain = false;
        boolean disconnect = false;
        synchronized (state) {
            if (state.queue.size() >= capacity) {
                if (!replaceCoalescible(state.queue, event)) {
                    boolean freed = removeOneCoalescible(state.queue);
                    if (!freed) {
                        disconnect = true;
                    } else {
                        state.queue.addLast(event);
                    }
                }
            } else {
                state.queue.addLast(event);
            }
            if (!disconnect && !state.draining) {
                state.draining = true;
                scheduleDrain = true;
            }
        }
        if (disconnect) {
            closeSlowConsumer(state);
        } else if (scheduleDrain) {
            scheduler.schedule(() -> drain(state), Instant.now());
        }
    }

    void close(String sessionId) {
        State removed;
        synchronized (states) {
            removed = states.remove(sessionId);
        }
        if (removed != null) {
            synchronized (removed) {
                removed.queue.clear();
                removed.draining = false;
            }
        }
    }

    private void drain(State state) {
        while (true) {
            RealtimeMessageMapper.ServerEvent event;
            synchronized (state) {
                event = state.queue.pollFirst();
                if (event == null) {
                    state.draining = false;
                    return;
                }
            }
            try {
                if (!state.session.isOpen()) {
                    close(state.session.getId());
                    return;
                }
                synchronized (state.session) {
                    state.session.sendMessage(new TextMessage(mapper.write(event)));
                }
            } catch (IOException | RuntimeException exception) {
                close(state.session.getId());
                safeClose(state.session, CloseStatus.SERVER_ERROR);
                return;
            }
        }
    }

    private static boolean replaceCoalescible(
            ArrayDeque<RealtimeMessageMapper.ServerEvent> queue,
            RealtimeMessageMapper.ServerEvent replacement) {
        if (!replacement.coalescible() || replacement.coalescingKey() == null) {
            return false;
        }
        Iterator<RealtimeMessageMapper.ServerEvent> iterator = queue.iterator();
        while (iterator.hasNext()) {
            var current = iterator.next();
            if (current.coalescible()
                    && replacement.coalescingKey().equals(current.coalescingKey())) {
                iterator.remove();
                queue.addLast(replacement);
                return true;
            }
        }
        return false;
    }

    private static boolean removeOneCoalescible(
            ArrayDeque<RealtimeMessageMapper.ServerEvent> queue) {
        Iterator<RealtimeMessageMapper.ServerEvent> iterator = queue.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().coalescible()) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    private void closeSlowConsumer(State state) {
        close(state.session.getId());
        safeClose(state.session, SLOW_CONSUMER);
    }

    private static void safeClose(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException ignored) {
            // Transport is already unavailable.
        }
    }

    private static final class State {
        private final WebSocketSession session;
        private final ArrayDeque<RealtimeMessageMapper.ServerEvent> queue = new ArrayDeque<>();
        private boolean draining;

        private State(WebSocketSession session) {
            this.session = session;
        }
    }
}

package com.cryptostrategy.platform.api.realtime;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Per-connection logical subscription ownership and bounded activation state. */
@Component
public final class SubscriptionRegistry {
    private final int maximumCandles;
    private final int maximumWorkloads;
    private final int pendingCapacity;
    private final Map<String, Map<String, Entry>> sessions = new HashMap<>();

    public SubscriptionRegistry(int maximumCandles, int maximumWorkloads) {
        this(maximumCandles, maximumWorkloads, 128);
    }

    @Autowired
    public SubscriptionRegistry(
            @Value("${platform.realtime.max-candle-subscriptions:4}") int maximumCandles,
            @Value("${platform.realtime.max-workload-subscriptions:4}") int maximumWorkloads,
            @Value("${platform.realtime.outbound-buffer-capacity:128}") int pendingCapacity) {
        if (maximumCandles < 1 || maximumWorkloads < 1 || pendingCapacity < 1) {
            throw new IllegalArgumentException("Realtime subscription limits must be positive");
        }
        this.maximumCandles = maximumCandles;
        this.maximumWorkloads = maximumWorkloads;
        this.pendingCapacity = pendingCapacity;
    }

    synchronized Entry reserve(
            String sessionId,
            String subscriptionId,
            Type type,
            String resourceId,
            Consumer<RealtimeMessageMapper.ServerEvent> delivery) {
        Map<String, Entry> entries = sessions.computeIfAbsent(sessionId, ignored -> new HashMap<>());
        if (entries.containsKey(subscriptionId)) {
            throw new RealtimeProtocolException(
                    "DUPLICATE_SUBSCRIPTION_ID", "Subscription ID is already active", false);
        }
        long sameFamily = entries.values().stream().filter(entry -> entry.type.family == type.family).count();
        int limit = type.family == Family.CANDLE ? maximumCandles : maximumWorkloads;
        if (sameFamily >= limit) {
            throw new RealtimeProtocolException(
                    type.family == Family.CANDLE
                            ? "MARKET_SUBSCRIPTION_LIMIT_EXCEEDED"
                            : "WORKLOAD_SUBSCRIPTION_LIMIT_EXCEEDED",
                    "Subscription limit exceeded",
                    false);
        }
        Entry entry = new Entry(type, resourceId, delivery);
        entries.put(subscriptionId, entry);
        return entry;
    }

    synchronized void activate(
            String sessionId, String subscriptionId, Entry expected, Runnable confirmation) {
        Entry entry = require(sessionId, subscriptionId, expected);
        if (entry.overflowed) {
            throw new RealtimeProtocolException("SUBSCRIPTION_FAILED",
                    "Subscription synchronization overflowed; resubscribe and refresh the snapshot", false, true);
        }
        // Confirmation and buffered events enter the outbound queue before live callbacks.
        confirmation.run();
        require(sessionId, subscriptionId, expected);
        while (!entry.pending.isEmpty()) {
            entry.delivery.accept(entry.pending.removeFirst());
            require(sessionId, subscriptionId, expected);
        }
        entry.active = true;
    }

    synchronized void attach(String sessionId, String subscriptionId, Entry expected, AutoCloseable handle) {
        Entry entry;
        try {
            entry = require(sessionId, subscriptionId, expected);
        } catch (RealtimeProtocolException exception) {
            // The connection may have closed while the upstream subscription was opening.
            closeHandle(handle);
            throw exception;
        }
        entry.handle = handle == null ? () -> {} : handle;
    }

    synchronized void discard(String sessionId, String subscriptionId, Entry expected) {
        Map<String, Entry> entries = sessions.get(sessionId);
        Entry entry = entries == null ? null : entries.get(subscriptionId);
        if (entry == expected) {
            remove(sessionId, subscriptionId, entry.type);
        }
    }

    synchronized void publish(
            String sessionId,
            String subscriptionId,
            Entry expected,
            RealtimeMessageMapper.ServerEvent event) {
        Map<String, Entry> entries = sessions.get(sessionId);
        Entry entry = entries == null ? null : entries.get(subscriptionId);
        // Provider callbacks can race a successful unsubscribe. A late transient event is
        // discarded because the authorized REST snapshot remains the recovery boundary.
        if (entry == null || entry != expected || entry.overflowed) {
            return;
        }
        if (!entry.active) {
            if (entry.pending.size() >= pendingCapacity) {
                entry.pending.clear();
                entry.overflowed = true;
                return;
            }
            entry.pending.addLast(event);
            return;
        }
        entry.delivery.accept(event);
    }

    synchronized Type remove(String sessionId, String subscriptionId, Type expected) {
        Map<String, Entry> entries = sessions.get(sessionId);
        Entry entry = entries == null ? null : entries.get(subscriptionId);
        if (entry == null || entry.type != expected) {
            throw new RealtimeProtocolException(
                    "SUBSCRIPTION_NOT_FOUND", "Subscription is not active", false);
        }
        entries.remove(subscriptionId);
        close(entry);
        if (entries.isEmpty()) {
            sessions.remove(sessionId);
        }
        return entry.type;
    }

    synchronized void closeSession(String sessionId) {
        Map<String, Entry> entries = sessions.remove(sessionId);
        if (entries != null) {
            entries.values().forEach(SubscriptionRegistry::close);
        }
    }

    private Entry require(String sessionId, String subscriptionId, Entry expected) {
        Map<String, Entry> entries = sessions.get(sessionId);
        Entry entry = entries == null ? null : entries.get(subscriptionId);
        if (entry == null || entry != expected) {
            throw new RealtimeProtocolException(
                    "SUBSCRIPTION_NOT_FOUND", "Subscription is not active", false);
        }
        return entry;
    }

    private static void close(Entry entry) {
        entry.pending.clear();
        closeHandle(entry.handle);
    }

    private static void closeHandle(AutoCloseable handle) {
        if (handle == null) {
            return;
        }
        try {
            handle.close();
        } catch (Exception ignored) {
            // Cleanup is best effort; no provider detail crosses the public boundary.
        }
    }

    enum Family { CANDLE, WORKLOAD }

    enum Type {
        CANDLES(Family.CANDLE),
        EXPERIMENT(Family.WORKLOAD),
        LEADERBOARD(Family.WORKLOAD);

        private final Family family;

        Type(Family family) {
            this.family = family;
        }
    }

    static final class Entry {
        private final Type type;
        private final String resourceKey;
        private final Consumer<RealtimeMessageMapper.ServerEvent> delivery;
        private final ArrayDeque<RealtimeMessageMapper.ServerEvent> pending = new ArrayDeque<>();
        private AutoCloseable handle = () -> {};
        private boolean active;
        private boolean overflowed;

        private Entry(
                Type type,
                String resourceId,
                Consumer<RealtimeMessageMapper.ServerEvent> delivery) {
            this.type = Objects.requireNonNull(type, "type");
            this.resourceKey = Objects.requireNonNull(resourceId, "resourceId");
            this.delivery = Objects.requireNonNull(delivery, "delivery");
        }

        String resourceId() {
            return resourceKey;
        }
    }
}

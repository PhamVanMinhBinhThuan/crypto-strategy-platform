package com.cryptostrategy.platform.marketdata.internal.realtime;

import com.cryptostrategy.platform.marketdata.api.event.CandleUpdateHandler;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionState;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionStateHandler;
import com.cryptostrategy.platform.marketdata.api.model.CandleSubscription;
import com.cryptostrategy.platform.marketdata.api.model.RealtimeCandleQuery;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketDataProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SharedSubscriptionRegistry {
    private final MarketDataProvider provider; private final Map<RealtimeCandleQuery, Entry> entries = new HashMap<>();
    public SharedSubscriptionRegistry(MarketDataProvider provider) { this.provider = provider; }
    public synchronized CandleSubscription subscribe(RealtimeCandleQuery query, CandleUpdateHandler updates, ConnectionStateHandler states) {
        Entry entry = entries.get(query);
        if (entry == null) {
            entry = new Entry(); Entry created = entry;
            entry.updates.add(updates); entry.states.add(states);
            entry.upstream = provider.subscribe(query, update -> created.updatesSnapshot().forEach(handler -> handler.onUpdate(update)),
                    state -> created.statesSnapshot().forEach(handler -> handler.onState(state)));
            entries.put(query, entry);
        } else {
            entry.updates.add(updates); entry.states.add(states);
        }
        Entry accepted = entry;
        return () -> release(query, accepted, updates, states);
    }
    private synchronized void release(RealtimeCandleQuery query, Entry entry, CandleUpdateHandler updates, ConnectionStateHandler states) {
        entry.updates.remove(updates); entry.states.remove(states);
        if (entry.updates.isEmpty()) { entries.remove(query); entry.upstream.close(); states.onState(ConnectionState.DISCONNECTED); }
    }
    public synchronized int upstreamCount() { return entries.size(); }
    private static final class Entry {
        private final List<CandleUpdateHandler> updates = new ArrayList<>(); private final List<ConnectionStateHandler> states = new ArrayList<>(); private CandleSubscription upstream;
        private List<CandleUpdateHandler> updatesSnapshot() { synchronized (this) { return List.copyOf(updates); } }
        private List<ConnectionStateHandler> statesSnapshot() { synchronized (this) { return List.copyOf(states); } }
    }
}

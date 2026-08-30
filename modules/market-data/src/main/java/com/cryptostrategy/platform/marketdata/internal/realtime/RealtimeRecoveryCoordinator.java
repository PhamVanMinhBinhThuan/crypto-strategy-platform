package com.cryptostrategy.platform.marketdata.internal.realtime;

import com.cryptostrategy.platform.marketdata.api.event.CandleUpdateHandler;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionStateHandler;
import com.cryptostrategy.platform.marketdata.api.model.CandleSubscription;
import com.cryptostrategy.platform.marketdata.api.model.RealtimeCandleQuery;

public final class RealtimeRecoveryCoordinator {
    private final SharedSubscriptionRegistry registry;
    public RealtimeRecoveryCoordinator(SharedSubscriptionRegistry registry) { this.registry = registry; }
    public CandleSubscription subscribe(RealtimeCandleQuery query, CandleUpdateHandler updates, ConnectionStateHandler states) {
        return registry.subscribe(query, updates, states);
    }
}

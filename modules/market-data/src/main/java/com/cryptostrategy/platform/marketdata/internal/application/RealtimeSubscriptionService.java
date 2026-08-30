package com.cryptostrategy.platform.marketdata.internal.application;

import com.cryptostrategy.platform.marketdata.api.event.CandleUpdateHandler;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionStateHandler;
import com.cryptostrategy.platform.marketdata.api.model.CandleSubscription;
import com.cryptostrategy.platform.marketdata.api.model.RealtimeCandleQuery;
import com.cryptostrategy.platform.marketdata.api.port.in.SubscribeCandlesUseCase;
import com.cryptostrategy.platform.marketdata.api.port.out.ClosedCandleStore;
import com.cryptostrategy.platform.marketdata.internal.realtime.SharedSubscriptionRegistry;

public final class RealtimeSubscriptionService implements SubscribeCandlesUseCase {
    private final SharedSubscriptionRegistry registry; private final ClosedCandleStore candles;
    public RealtimeSubscriptionService(SharedSubscriptionRegistry registry, ClosedCandleStore candles) { this.registry = registry; this.candles = candles; }
    @Override public CandleSubscription subscribeCandles(RealtimeCandleQuery query, CandleUpdateHandler updates, ConnectionStateHandler states) {
        return registry.subscribe(query, update -> { if (update.candle().closed()) candles.saveClosed(update.candle()); updates.onUpdate(update); }, states);
    }
}

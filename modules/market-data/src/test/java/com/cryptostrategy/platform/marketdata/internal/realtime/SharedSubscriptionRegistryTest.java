package com.cryptostrategy.platform.marketdata.internal.realtime;

import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.PAIR;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionState;
import com.cryptostrategy.platform.marketdata.api.event.CandleUpdateHandler;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionStateHandler;
import com.cryptostrategy.platform.marketdata.api.model.CandleSubscription;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;
import com.cryptostrategy.platform.marketdata.api.model.RealtimeCandleQuery;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SharedSubscriptionRegistryTest {
    @Test
    void sharesOneUpstreamAndClosesItAfterLastConsumerLeaves() {
        FakeProvider provider = new FakeProvider();
        SharedSubscriptionRegistry registry = new SharedSubscriptionRegistry(provider);
        RealtimeCandleQuery query = new RealtimeCandleQuery(MarketProvider.BINANCE, PAIR, Timeframe.ONE_MINUTE);

        CandleSubscription first = registry.subscribe(query, update -> { }, state -> { });
        CandleSubscription second = registry.subscribe(query, update -> { }, state -> { });
        assertEquals(1, provider.opens);
        assertEquals(1, registry.upstreamCount());

        first.close();
        assertEquals(0, provider.closes);
        second.close();
        assertEquals(1, provider.closes);
        assertEquals(0, registry.upstreamCount());
    }

    @Test
    void replaysCurrentUpstreamStateToLateConsumer() {
        FakeProvider provider = new FakeProvider();
        SharedSubscriptionRegistry registry = new SharedSubscriptionRegistry(provider);
        RealtimeCandleQuery query = new RealtimeCandleQuery(
                MarketProvider.BINANCE, PAIR, Timeframe.ONE_MINUTE);
        List<ConnectionState> firstStates = new ArrayList<>();
        List<ConnectionState> lateStates = new ArrayList<>();

        registry.subscribe(query, update -> { }, firstStates::add);
        registry.subscribe(query, update -> { }, lateStates::add);

        assertEquals(List.of(ConnectionState.CONNECTING, ConnectionState.CONNECTED), firstStates);
        assertEquals(List.of(ConnectionState.CONNECTED), lateStates);
        assertEquals(1, provider.opens);
    }

    private static final class FakeProvider implements MarketDataProvider {
        private int opens;
        private int closes;
        @Override public MarketProvider providerId() { return MarketProvider.BINANCE; }
        @Override public String normalizationVersion() { return "test-v1"; }
        @Override public HistoricalCandleBatch loadHistorical(HistoricalCandleQuery query) { return new HistoricalCandleBatch(List.of()); }
        @Override public CandleSubscription subscribe(RealtimeCandleQuery query, CandleUpdateHandler updates, ConnectionStateHandler states) {
            opens++;
            states.onState(ConnectionState.CONNECTING);
            states.onState(ConnectionState.CONNECTED);
            return () -> closes++;
        }
    }
}

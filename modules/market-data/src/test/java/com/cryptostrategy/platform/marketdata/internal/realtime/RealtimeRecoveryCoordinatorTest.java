package com.cryptostrategy.platform.marketdata.internal.realtime;

import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.PAIR;
import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.START;
import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.candle;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.marketdata.api.event.CandleUpdate;
import com.cryptostrategy.platform.marketdata.api.event.CandleUpdateHandler;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionState;
import com.cryptostrategy.platform.marketdata.api.event.ConnectionStateHandler;
import com.cryptostrategy.platform.marketdata.api.model.CandleSubscription;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleBatch;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;
import com.cryptostrategy.platform.marketdata.api.model.RealtimeCandleQuery;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketDataProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RealtimeRecoveryCoordinatorTest {
    private static final RealtimeCandleQuery QUERY =
            new RealtimeCandleQuery(MarketProvider.BINANCE, PAIR, Timeframe.ONE_MINUTE);

    @Test
    void reconnectsBackfillsGapAndSuppressesDuplicateOverlap() {
        FakeProvider provider = new FakeProvider();
        provider.historical = new HistoricalCandleBatch(List.of(candle(0, "1"), candle(1, "2")));
        List<CandleUpdate> received = new ArrayList<>();
        List<ConnectionState> states = new ArrayList<>();
        List<Duration> delays = new ArrayList<>();
        var coordinator = new RealtimeRecoveryCoordinator(
                provider,
                Clock.fixed(START.plusSeconds(180), ZoneOffset.UTC),
                policy(3, delays));

        CandleSubscription subscription = coordinator.subscribe(QUERY, received::add, states::add);
        provider.connections.getFirst().emit(candleUpdate(0, "1"));
        provider.emitOnNextConnect = candleUpdate(2, "3");
        provider.connections.getFirst().state(ConnectionState.RECONNECTING);

        assertEquals(List.of(0L, 60L, 120L), received.stream()
                .map(update -> update.candle().key().openTime().getEpochSecond() - START.getEpochSecond())
                .toList());
        assertEquals(2, provider.connections.size());
        assertEquals(List.of(Duration.ofMillis(500)), delays);
        assertEquals(ConnectionState.CONNECTED, states.getLast());
        subscription.close();
    }

    @Test
    void stopsAfterBoundedReconnectAttempts() {
        FakeProvider provider = new FakeProvider();
        List<ConnectionState> states = new ArrayList<>();
        List<Duration> delays = new ArrayList<>();
        var coordinator = new RealtimeRecoveryCoordinator(
                provider,
                Clock.fixed(START.plusSeconds(60), ZoneOffset.UTC),
                policy(3, delays));

        coordinator.subscribe(QUERY, update -> { }, states::add);
        provider.failNewConnections = true;
        provider.connections.getFirst().state(ConnectionState.RECONNECTING);

        assertEquals(4, provider.subscribeAttempts);
        assertEquals(List.of(
                Duration.ofMillis(500),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)), delays);
        assertEquals(ConnectionState.DISCONNECTED, states.getLast());
    }

    private static RealtimeRecoveryCoordinator.RecoveryPolicy policy(
            int maxAttempts,
            List<Duration> delays) {
        return new RealtimeRecoveryCoordinator.RecoveryPolicy(
                maxAttempts,
                Duration.ofMillis(500),
                Duration.ofSeconds(5),
                1000,
                10,
                delays::add,
                Runnable::run,
                () -> 0.5d);
    }

    private static CandleUpdate candleUpdate(int minute, String close) {
        var value = candle(minute, close);
        return new CandleUpdate(value, value.closeTime());
    }

    private static final class FakeProvider implements MarketDataProvider {
        private final List<Connection> connections = new ArrayList<>();
        private HistoricalCandleBatch historical = new HistoricalCandleBatch(List.of());
        private CandleUpdate emitOnNextConnect;
        private boolean failNewConnections;
        private int subscribeAttempts;

        @Override
        public MarketProvider providerId() {
            return MarketProvider.BINANCE;
        }

        @Override
        public String normalizationVersion() {
            return "fixture-v1";
        }

        @Override
        public HistoricalCandleBatch loadHistorical(HistoricalCandleQuery query) {
            return historical;
        }

        @Override
        public CandleSubscription subscribe(
                RealtimeCandleQuery query,
                CandleUpdateHandler updates,
                ConnectionStateHandler states) {
            subscribeAttempts++;
            if (failNewConnections) {
                throw new IllegalStateException("simulated disconnect");
            }
            Connection connection = new Connection(updates, states);
            connections.add(connection);
            states.onState(ConnectionState.CONNECTING);
            if (emitOnNextConnect != null) {
                CandleUpdate buffered = emitOnNextConnect;
                emitOnNextConnect = null;
                updates.onUpdate(buffered);
            }
            states.onState(ConnectionState.CONNECTED);
            return connection;
        }
    }

    private static final class Connection implements CandleSubscription {
        private final CandleUpdateHandler updates;
        private final ConnectionStateHandler states;
        private boolean closed;

        private Connection(CandleUpdateHandler updates, ConnectionStateHandler states) {
            this.updates = updates;
            this.states = states;
        }

        private void emit(CandleUpdate update) {
            updates.onUpdate(update);
        }

        private void state(ConnectionState state) {
            states.onState(state);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}

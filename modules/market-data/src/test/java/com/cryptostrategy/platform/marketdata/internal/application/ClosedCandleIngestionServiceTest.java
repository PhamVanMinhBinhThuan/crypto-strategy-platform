package com.cryptostrategy.platform.marketdata.internal.application;

import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.candle;
import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.marketdata.api.port.out.ClosedCandleStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClosedCandleIngestionServiceTest {
    @Test void rejectsWholeBatchBeforePersistenceWhenAnyCandleIsOpen() {
        List<Candle> calls = new ArrayList<>();
        ClosedCandleStore store = new ClosedCandleStore() {
            public com.cryptostrategy.platform.marketdata.api.model.PersistedCandle saveClosed(Candle value) { calls.add(value); return null; }
            public List<com.cryptostrategy.platform.marketdata.api.model.PersistedCandle> saveClosedBatch(List<Candle> values) { calls.addAll(values); return List.of(); }
            public List<com.cryptostrategy.platform.marketdata.api.model.PersistedCandle> findRange(
                    com.cryptostrategy.platform.domain.api.market.MarketProvider provider,
                    com.cryptostrategy.platform.domain.api.market.TradingPairId pair,
                    com.cryptostrategy.platform.domain.api.market.Timeframe timeframe,
                    java.time.Instant start, java.time.Instant end) { return List.of(); }
        };
        Candle closed = candle(0, "1");
        Candle open = new Candle(closed.key(), closed.closeTime(), closed.open(), closed.high(), closed.low(), closed.close(), closed.volume(), false);
        assertThrows(RuntimeException.class, () -> new ClosedCandleIngestionService(store).accept(List.of(closed, open)));
        assertTrue(calls.isEmpty());
    }
}

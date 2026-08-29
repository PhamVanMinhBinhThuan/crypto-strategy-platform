package com.cryptostrategy.platform.marketdata.internal.provider.binance;

import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.model.RealtimeCandleQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BinanceCandleMapperTest {
    private final BinanceCandleMapper mapper = new BinanceCandleMapper(new ObjectMapper());

    @Test void normalizesHistoricalTupleIntoClosedCanonicalCandle() throws Exception {
        var tuple = new ObjectMapper().readTree("[1767225600000,\"1.000\",\"3\",\"0.5\",\"2.00\",\"10.000\",1767225659999]");
        var candle = mapper.mapHistorical(tuple, query(1));
        assertTrue(candle.closed());
        assertEquals(START, candle.key().openTime());
        assertEquals("2", candle.close().toPlainString());
    }

    @Test void mapsFinalityAndEventTimeFromWebsocketFixture() {
        String payload = "{\"E\":1767225659000,\"s\":\"BTCUSDT\",\"k\":{\"t\":1767225600000,\"T\":1767225659999,\"i\":\"1m\",\"o\":\"1\",\"h\":\"3\",\"l\":\"0.5\",\"c\":\"2\",\"v\":\"10\",\"x\":true}}";
        var update = mapper.mapStream(payload, new RealtimeCandleQuery(com.cryptostrategy.platform.domain.api.market.MarketProvider.BINANCE,
                PAIR, com.cryptostrategy.platform.domain.api.market.Timeframe.ONE_MINUTE));
        assertTrue(update.candle().closed());
        assertEquals("2026-01-01T00:00:59Z", update.providerEventTime().toString());
    }

    @Test void malformedTupleProducesStableMappingError() throws Exception {
        MarketDataException error = assertThrows(MarketDataException.class,
                () -> mapper.mapHistorical(new ObjectMapper().readTree("[1]"), query(1)));
        assertEquals(MarketDataErrorCode.MARKET_DATA_MAPPING_FAILED, error.code());
    }
}

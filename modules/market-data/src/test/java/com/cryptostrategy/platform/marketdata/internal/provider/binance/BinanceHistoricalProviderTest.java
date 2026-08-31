package com.cryptostrategy.platform.marketdata.internal.provider.binance;

import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.query;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cryptostrategy.platform.marketdata.internal.provider.binance.transport.BinanceRestTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BinanceHistoricalProviderTest {
    @Test
    void sendsExclusiveEndAsEndMinusOneMillisecondAndReturnsOrderedCanonicalCandles() {
        RecordingTransport transport = new RecordingTransport(List.of(response(
                "[[1767225660000,\"2\",\"3\",\"1\",\"2\",\"10\",1767225719999]," +
                "[1767225600000,\"1\",\"2\",\"0.5\",\"1.5\",\"8\",1767225659999]]")));
        var provider = new BinanceHistoricalProvider(
                transport, new BinanceCandleMapper(new ObjectMapper()), "binance-v1", null);

        var batch = provider.loadHistorical(query(2));

        assertEquals(2, batch.candles().size());
        assertEquals(query(2).startTime(), batch.candles().getFirst().key().openTime());
        assertEquals(query(2).endTime().toEpochMilli() - 1, transport.calls.getFirst().endTime());
        assertEquals("BTCUSDT", transport.calls.getFirst().symbol());
        assertEquals("1m", transport.calls.getFirst().interval());
    }

    private static BinanceRestTransport.Response response(String body) {
        return new BinanceRestTransport.Response(200, body, Map.of());
    }

    private static final class RecordingTransport implements BinanceRestTransport {
        private final List<Response> responses;
        private final List<Call> calls = new ArrayList<>();
        private int index;

        private RecordingTransport(List<Response> responses) { this.responses = responses; }

        @Override public Response getKlines(String symbol, String interval, long startTime, long endTime, int limit) {
            calls.add(new Call(symbol, interval, startTime, endTime, limit));
            return responses.get(index++);
        }
        @Override public URI baseUri() { return URI.create("https://api.binance.test"); }
    }

    private record Call(String symbol, String interval, long startTime, long endTime, int limit) { }
}

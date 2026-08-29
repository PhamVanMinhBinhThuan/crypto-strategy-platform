package com.cryptostrategy.platform.api.config;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MarketDataPropertiesTest {
    @Test void acceptsBoundedPublicBinanceConfiguration() {
        var properties = new MarketDataProperties("binance", "binance-v1", "candle-v1",
                binance(URI.create("https://api.binance.com"), URI.create("wss://stream.binance.com:9443"), 1000));
        assertEquals("binance", properties.provider());
    }
    @Test void rejectsSecretsUnsafeSchemesAndUnboundedPages() {
        assertThrows(IllegalArgumentException.class, () -> binance(URI.create("http://api.binance.com"), URI.create("wss://stream.binance.com"), 1000));
        assertThrows(IllegalArgumentException.class, () -> binance(URI.create("https://user:secret@api.binance.com"), URI.create("wss://stream.binance.com"), 1000));
        assertThrows(IllegalArgumentException.class, () -> binance(URI.create("https://api.binance.com"), URI.create("wss://stream.binance.com"), 1001));
    }
    @Test void rejectsUnsupportedChecksumContract() {
        assertThrows(IllegalArgumentException.class, () -> new MarketDataProperties("fixture", "fixture-v1", "candle-v2", null));
    }
    private static MarketDataProperties.Binance binance(URI rest, URI websocket, int pageSize) {
        return new MarketDataProperties.Binance(rest, websocket, Duration.ofSeconds(2), Duration.ofSeconds(5), pageSize, 10,
                new MarketDataProperties.Retry(3, Duration.ofMillis(10), Duration.ofSeconds(1)),
                new MarketDataProperties.Reconnect(3, Duration.ofMillis(10), Duration.ofSeconds(1)));
    }
}

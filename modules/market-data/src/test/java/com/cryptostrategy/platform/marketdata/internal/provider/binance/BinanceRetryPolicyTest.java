package com.cryptostrategy.platform.marketdata.internal.provider.binance;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.marketdata.internal.provider.binance.transport.BinanceRestTransport;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BinanceRetryPolicyTest {
    @Test void retriesRateLimitWithBoundedRetryAfterAndThenSucceeds() {
        List<Duration> sleeps = new ArrayList<>();
        BinanceRetryPolicy policy = new BinanceRetryPolicy(3, Duration.ofMillis(10), Duration.ofSeconds(2), sleeps::add);
        AtomicInteger calls = new AtomicInteger();
        var response = policy.execute(() -> calls.getAndIncrement() == 0
                ? new BinanceRestTransport.Response(429, "", Map.of("Retry-After", List.of("30")))
                : new BinanceRestTransport.Response(200, "[]", Map.of()));
        assertEquals(200, response.status());
        assertEquals(2, calls.get());
        assertEquals(List.of(Duration.ofSeconds(2)), sleeps);
    }

    @Test void stopsAtConfiguredAttemptBound() {
        AtomicInteger calls = new AtomicInteger();
        BinanceRetryPolicy policy = new BinanceRetryPolicy(2, Duration.ZERO, Duration.ZERO, ignored -> { });
        var response = policy.execute(() -> { calls.incrementAndGet(); return new BinanceRestTransport.Response(500, "", Map.of()); });
        assertEquals(500, response.status());
        assertEquals(2, calls.get());
    }
}

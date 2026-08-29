package com.cryptostrategy.platform.marketdata.internal.provider.binance;

import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.internal.provider.binance.transport.BinanceRestTransport;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class BinanceRetryPolicy {
    @FunctionalInterface public interface Sleeper { void sleep(Duration duration) throws InterruptedException; }
    private final int maxAttempts;
    private final Duration baseDelay;
    private final Duration maxDelay;
    private final Sleeper sleeper;

    public BinanceRetryPolicy(int maxAttempts, Duration baseDelay, Duration maxDelay, Sleeper sleeper) {
        if (maxAttempts < 1 || baseDelay.isNegative() || maxDelay.isNegative() || baseDelay.compareTo(maxDelay) > 0) {
            throw new IllegalArgumentException("Invalid Binance retry bounds");
        }
        this.maxAttempts = maxAttempts; this.baseDelay = baseDelay; this.maxDelay = maxDelay; this.sleeper = sleeper;
    }
    public static BinanceRetryPolicy defaults() {
        return new BinanceRetryPolicy(3, Duration.ofMillis(250), Duration.ofSeconds(5), Thread::sleep);
    }
    public BinanceRestTransport.Response execute(Supplier<BinanceRestTransport.Response> operation) {
        for (int attempt = 1; ; attempt++) {
            try {
                BinanceRestTransport.Response response = operation.get();
                if (!retryable(response.status()) || attempt == maxAttempts) return response;
                pause(delay(response, attempt));
            } catch (MarketDataException exception) {
                if (exception.code() != MarketDataErrorCode.MARKET_PROVIDER_UNAVAILABLE || attempt == maxAttempts) throw exception;
                pause(backoff(attempt));
            }
        }
    }
    private Duration delay(BinanceRestTransport.Response response, int attempt) {
        for (var header : response.headers().entrySet()) {
            if (header.getKey().toLowerCase(Locale.ROOT).equals("retry-after")) {
                List<String> values = header.getValue();
                if (!values.isEmpty()) {
                    try { return Duration.ofSeconds(Math.min(Long.parseLong(values.getFirst()), maxDelay.toSeconds())); }
                    catch (NumberFormatException ignored) { }
                }
            }
        }
        return backoff(attempt);
    }
    private Duration backoff(int attempt) {
        long multiplier = 1L << Math.min(attempt - 1, 30);
        Duration candidate;
        try { candidate = baseDelay.multipliedBy(multiplier); }
        catch (ArithmeticException ignored) { candidate = maxDelay; }
        return candidate.compareTo(maxDelay) > 0 ? maxDelay : candidate;
    }
    private void pause(Duration duration) {
        try { sleeper.sleep(duration); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MarketDataException(MarketDataErrorCode.MARKET_PROVIDER_UNAVAILABLE, "Provider retry interrupted", java.util.Map.of(), exception);
        }
    }
    private static boolean retryable(int status) { return status == 418 || status == 429 || status >= 500; }
}

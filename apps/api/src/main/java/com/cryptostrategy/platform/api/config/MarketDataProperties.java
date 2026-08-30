package com.cryptostrategy.platform.api.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("platform.market-data")
public record MarketDataProperties(String provider, String normalizationVersion, String checksumContractVersion,
                                   Binance binance) {
    public MarketDataProperties {
        requireText(provider, "provider");
        requireText(normalizationVersion, "normalizationVersion");
        if (!"candle-v1".equals(checksumContractVersion)) throw new IllegalArgumentException("Unsupported checksum contract version");
        if (!provider.equalsIgnoreCase("fixture") && !provider.equalsIgnoreCase("binance")) throw new IllegalArgumentException("Unsupported Market Data provider");
        if (provider.equalsIgnoreCase("binance") && binance == null) throw new IllegalArgumentException("Binance configuration is required");
    }
    public record Binance(URI restBaseUrl, URI websocketBaseUrl, Duration connectTimeout, Duration requestTimeout,
                          int pageSize, int maxPages, Retry retry, Reconnect reconnect) {
        public Binance {
            safePublicUri(restBaseUrl, "https", "REST");
            safePublicUri(websocketBaseUrl, "wss", "WebSocket");
            positive(connectTimeout, "connectTimeout"); positive(requestTimeout, "requestTimeout");
            if (pageSize < 1 || pageSize > 1000 || maxPages < 1) throw new IllegalArgumentException("Invalid Binance pagination bounds");
            if (retry == null || reconnect == null) throw new IllegalArgumentException("Binance retry and reconnect bounds are required");
        }
    }
    public record Retry(int maxAttempts, Duration initialDelay, Duration maxDelay) {
        public Retry { if (maxAttempts < 1) throw new IllegalArgumentException("Invalid retry attempts"); positive(initialDelay, "retry.initialDelay"); positive(maxDelay, "retry.maxDelay"); }
    }
    public record Reconnect(int maxAttempts, Duration initialDelay, Duration maxDelay) {
        public Reconnect { if (maxAttempts < 1) throw new IllegalArgumentException("Invalid reconnect attempts"); positive(initialDelay, "reconnect.initialDelay"); positive(maxDelay, "reconnect.maxDelay"); }
    }
    private static void safePublicUri(URI uri, String scheme, String name) {
        if (uri == null || !scheme.equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Invalid public Binance " + name + " endpoint");
        }
    }
    private static void positive(Duration value, String name) { if (value == null || value.isZero() || value.isNegative()) throw new IllegalArgumentException(name + " must be positive"); }
    private static void requireText(String value, String name) { if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required"); }
}

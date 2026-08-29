package com.cryptostrategy.platform.marketdata.internal.provider.binance.transport;

import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public final class JdkBinanceRestTransport implements BinanceRestTransport {
    private final URI baseUri; private final HttpClient client; private final Duration timeout;
    public JdkBinanceRestTransport(URI baseUri, Duration connectTimeout, Duration timeout) {
        if (!"https".equalsIgnoreCase(baseUri.getScheme())) throw new IllegalArgumentException("Binance REST URL must use HTTPS");
        this.baseUri = baseUri; this.timeout = timeout; this.client = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    }
    @Override public Response getKlines(String symbol, String interval, long startTime, long endTime, int limit) {
        String query = "symbol=" + encode(symbol) + "&interval=" + encode(interval) + "&startTime=" + startTime
                + "&endTime=" + endTime + "&limit=" + limit + "&timeZone=0";
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/v3/klines?" + query)).timeout(timeout).GET().build();
        try { HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString()); return new Response(response.statusCode(), response.body(), response.headers().map()); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new MarketDataException(MarketDataErrorCode.MARKET_PROVIDER_UNAVAILABLE, "Provider request interrupted", Map.of(), exception); }
        catch (IOException exception) { throw new MarketDataException(MarketDataErrorCode.MARKET_PROVIDER_UNAVAILABLE, "Provider unavailable", Map.of(), exception); }
    }
    @Override public URI baseUri() { return baseUri; }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}

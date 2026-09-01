package com.cryptostrategy.platform.news.provider;

import com.cryptostrategy.platform.news.api.model.NewsSource;
import com.cryptostrategy.platform.news.api.model.ProviderNewsItem;
import com.cryptostrategy.platform.news.api.port.out.NewsProvider;
import com.cryptostrategy.platform.news.internal.provider.fixture.FixtureNewsProvider;
import com.cryptostrategy.platform.news.internal.provider.rss.RssNewsProvider;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.Nested;

class ConfiguredNewsProvidersTest {
    @Nested class FixtureContract extends NewsProviderContract {
        @Override protected NewsProvider provider(List<ProviderNewsItem> items) {
            return new FixtureNewsProvider(new NewsSource("fixture"), items);
        }
        @Override protected NewsProvider malformedProvider() { return failing(new IllegalArgumentException("malformed fixture")); }
        @Override protected NewsProvider timeoutProvider() { return failing(new IllegalStateException("fixture timeout")); }
    }

    @Nested class RssContract extends NewsProviderContract {
        @Override protected NewsProvider provider(List<ProviderNewsItem> items) {
            StringBuilder xml = new StringBuilder("<rss><channel>");
            for (var item : items) xml.append("<item><title>").append(item.titleHtml()).append("</title><description>")
                    .append(item.contentHtml()).append("</description><link>").append(item.url())
                    .append("</link><pubDate>").append(item.publishedAt()).append("</pubDate></item>");
            xml.append("</channel></rss>");
            return rss(new StaticHttpClient(xml.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), false));
        }
        @Override protected NewsProvider malformedProvider() {
            return rss(new StaticHttpClient("<rss><item>".getBytes(java.nio.charset.StandardCharsets.UTF_8), false));
        }
        @Override protected NewsProvider timeoutProvider() { return rss(new StaticHttpClient(new byte[0], true)); }
        private NewsProvider rss(HttpClient client) {
            return new RssNewsProvider(new NewsSource("rss"), URI.create("https://feed.example.test/rss"), "en",
                    List.of("BTC"), client, Duration.ofSeconds(1));
        }
    }

    private static NewsProvider failing(RuntimeException error) {
        return new NewsProvider() {
            @Override public NewsSource source() { return new NewsSource("failing"); }
            @Override public List<ProviderNewsItem> fetchSince(Instant since) { throw error; }
        };
    }

    private static final class StaticHttpClient extends HttpClient {
        private final byte[] body;
        private final boolean timeout;
        private StaticHttpClient(byte[] body, boolean timeout) { this.body = body.clone(); this.timeout = timeout; }
        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { return null; }
        @Override public SSLParameters sslParameters() { return new SSLParameters(); }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }
        @Override @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
                throws IOException {
            if (timeout) throw new HttpTimeoutException("controlled timeout");
            return (HttpResponse<T>) new StaticResponse(request, body);
        }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }
    }

    private record StaticResponse(HttpRequest request, byte[] body) implements HttpResponse<byte[]> {
        @Override public int statusCode() { return 200; }
        @Override public Optional<HttpResponse<byte[]>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (a, b) -> true); }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return request.uri(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
}

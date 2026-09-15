package com.cryptostrategy.platform.api.news;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class HttpSentimentAvailabilityProbe implements SentimentAvailabilityProbe {
    private static final int MAX_HEALTH_RESPONSE_BYTES = 8_192;

    private final HttpClient http;
    private final URI endpoint;
    private final ObjectMapper json;
    private final Duration timeout;

    @Autowired
    public HttpSentimentAvailabilityProbe(
            @Value("${platform.sentiment.base-url}") URI baseUrl,
            @Value("${platform.sentiment.status-timeout}") Duration timeout,
            ObjectMapper json) {
        this(
                HttpClient.newBuilder().connectTimeout(timeout).build(),
                baseUrl.resolve("/health/ready"),
                timeout,
                json);
    }

    HttpSentimentAvailabilityProbe(
            HttpClient http, URI endpoint, Duration timeout, ObjectMapper json) {
        this.http = Objects.requireNonNull(http, "http");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.json = Objects.requireNonNull(json, "json");
    }

    @Override
    public CompletionStage<Boolean> check() {
        var request = HttpRequest.newBuilder(endpoint).timeout(timeout).GET().build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .handle((response, error) -> error == null && isReady(response));
    }

    private boolean isReady(HttpResponse<byte[]> response) {
        if (response.statusCode() != 200 || response.body().length > MAX_HEALTH_RESPONSE_BYTES) {
            return false;
        }
        try {
            return "READY".equals(json.readTree(response.body()).path("status").asText());
        } catch (Exception invalidResponse) {
            return false;
        }
    }
}

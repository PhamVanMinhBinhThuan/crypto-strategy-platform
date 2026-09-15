package com.cryptostrategy.platform.api.news;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class HttpSentimentAvailabilityProbeTest {
    @Test
    void acceptsOnlyAReadyHealthResponse() {
        var http = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"status\":\"READY\"}".getBytes());
        when(http.sendAsync(
                        any(HttpRequest.class),
                        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenReturn(CompletableFuture.completedFuture(response));
        var probe = probe(http);

        assertTrue(probe.check().toCompletableFuture().join());
    }

    @Test
    void convertsConnectionFailureToDegradedState() {
        var http = mock(HttpClient.class);
        when(http.sendAsync(
                        any(HttpRequest.class),
                        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("offline")));
        var probe = probe(http);

        assertFalse(probe.check().toCompletableFuture().join());
    }

    private static HttpSentimentAvailabilityProbe probe(HttpClient http) {
        return new HttpSentimentAvailabilityProbe(
                http,
                URI.create("http://127.0.0.1:8000/health/ready"),
                Duration.ofSeconds(1),
                new ObjectMapper());
    }
}

package com.cryptostrategy.platform.worker.news.sentiment;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.news.api.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.*;
import java.util.concurrent.ExecutionException;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;

class HttpSentimentInferenceAdapterTest {
    private MockWebServer server;
    @BeforeEach void start() throws Exception {server=new MockWebServer();server.start();}
    @AfterEach void stop() throws Exception {server.shutdown();}
    @Test void sends_one_authorized_call_and_validates_echo() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type","application/json").setBody("""
          {"requestId":"01K4A000000000000000000001","newsId":"01K4A000000000000000000002","language":"en","contentHash":"sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef","contractVersion":"sentiment-v1","modelName":"multichannel-english","modelVersion":"v1","preprocessingVersion":"prep-v1","label":"POSITIVE","confidence":"0.82","polarityScore":"0.64","analyzedAt":"2026-08-30T10:00:00Z"}
          """));
        var release=new SentimentModelRelease("v1","multichannel-english","prep-v1","sentiment-v1");
        var request=new SentimentAnalysisRequest("01K4A000000000000000000001",new NewsId("01K4A000000000000000000002"),"title","content",LanguageCode.ENGLISH,new ContentHash("sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"),release);
        var adapter=new HttpSentimentInferenceAdapter(HttpClient.newHttpClient(),server.url("/").uri(),"secret-service-token",new ObjectMapper().findAndRegisterModules(),new SentimentContractMapper());
        assertEquals(SentimentLabel.POSITIVE,adapter.analyze(request).toCompletableFuture().get().label());
        var recorded=server.takeRequest();
        assertEquals("Bearer secret-service-token",recorded.getHeader("Authorization"));
        assertEquals(request.requestId(),recorded.getHeader("X-Correlation-Id"));
        assertEquals("/api/v1/sentiment/analyze",recorded.getPath());
        var outbound=new ObjectMapper().readTree(recorded.getBody().readUtf8());
        assertEquals("en",outbound.path("language").asText());
        assertEquals("sentiment-v1",outbound.path("contractVersion").asText());
        assertEquals(1,server.getRequestCount());
    }
    @Test void maps_503_retryable_without_internal_retry() {
        server.enqueue(new MockResponse().setResponseCode(503).setBody("{}"));
        var release=new SentimentModelRelease("v1","model","prep","sentiment-v1");
        var request=new SentimentAnalysisRequest("01K4A000000000000000000001",new NewsId("01K4A000000000000000000002"),"t","c",LanguageCode.ENGLISH,new ContentHash("sha256:"+"a".repeat(64)),release);
        var adapter=new HttpSentimentInferenceAdapter(HttpClient.newHttpClient(),server.url("/").uri(),"secret-service-token",new ObjectMapper(),new SentimentContractMapper());
        var error=assertThrows(java.util.concurrent.ExecutionException.class,()->adapter.analyze(request).toCompletableFuture().get());
        assertTrue(error.getCause() instanceof SentimentClientException e&&e.retryable());
        assertEquals(1,server.getRequestCount());
    }

    @Test void rejects_mismatched_success_and_oversized_bodies_as_permanent_circuit_failures() {
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type","application/json").setBody("""
          {"requestId":"01K4A000000000000000000001","newsId":"01K4A000000000000000000009","language":"en","contentHash":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","contractVersion":"sentiment-v1","modelName":"model","modelVersion":"v1","preprocessingVersion":"prep","label":"POSITIVE","confidence":"0.8","polarityScore":"0.6","analyzedAt":"2026-08-30T10:00:00Z"}
          """));
        var first=assertThrows(ExecutionException.class,()->adapter(request()).analyze(request()).toCompletableFuture().get());
        assertTrue(first.getCause() instanceof SentimentClientException error && !error.retryable() && error.countsTowardCircuit());

        server.enqueue(new MockResponse().setResponseCode(200).setBody("x".repeat(262_145)));
        var second=assertThrows(ExecutionException.class,()->adapter(request()).analyze(request()).toCompletableFuture().get());
        assertTrue(second.getCause() instanceof SentimentClientException error && !error.retryable() && error.countsTowardCircuit());
        assertEquals(2,server.getRequestCount());
    }

    @Test void maps_authentication_failure_permanently_without_exposing_response_content() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("secret server details"));
        var thrown=assertThrows(ExecutionException.class,()->adapter(request()).analyze(request()).toCompletableFuture().get());
        var error=assertInstanceOf(SentimentClientException.class,thrown.getCause());
        assertFalse(error.retryable());
        assertFalse(error.countsTowardCircuit());
        assertFalse(error.getMessage().contains("secret server details"));
    }

    private SentimentAnalysisRequest request(){
        return new SentimentAnalysisRequest("01K4A000000000000000000001",new NewsId("01K4A000000000000000000002"),"t","c",LanguageCode.ENGLISH,
                new ContentHash("sha256:"+"a".repeat(64)),new SentimentModelRelease("v1","model","prep","sentiment-v1"));
    }
    private HttpSentimentInferenceAdapter adapter(SentimentAnalysisRequest ignored){
        return new HttpSentimentInferenceAdapter(HttpClient.newHttpClient(),server.url("/").uri(),"secret-service-token",
                new ObjectMapper().findAndRegisterModules(),new SentimentContractMapper());
    }
}

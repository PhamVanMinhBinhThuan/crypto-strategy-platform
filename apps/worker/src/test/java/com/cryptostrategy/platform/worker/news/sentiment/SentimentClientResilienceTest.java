package com.cryptostrategy.platform.worker.news.sentiment;

import static org.junit.jupiter.api.Assertions.*;

import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.timelimiter.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.cryptostrategy.platform.news.api.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;

class SentimentClientResilienceTest {
    private ScheduledExecutorService scheduler;
    @BeforeEach void setUp() { scheduler = Executors.newSingleThreadScheduledExecutor(); }
    @AfterEach void tearDown() { scheduler.shutdownNow(); }

    @Test void records_each_admitted_permit_exactly_once_and_never_retries_internally() {
        var breaker = breaker();
        var calls = new AtomicInteger();
        assertEquals("ok", guard(breaker, Duration.ofSeconds(30), 2).execute(() -> {
            calls.incrementAndGet(); return CompletableFuture.completedFuture("ok");
        }).toCompletableFuture().join());
        assertEquals(1, breaker.getMetrics().getNumberOfSuccessfulCalls());

        assertThrows(CompletionException.class, () -> guard(breaker, Duration.ofSeconds(30), 2).execute(() -> {
            calls.incrementAndGet(); return CompletableFuture.failedFuture(new SentimentClientException("503", true, true));
        }).toCompletableFuture().join());
        assertEquals(1, breaker.getMetrics().getNumberOfFailedCalls());

        int buffered = breaker.getMetrics().getNumberOfBufferedCalls();
        assertThrows(CompletionException.class, () -> guard(breaker, Duration.ofSeconds(30), 2).execute(() -> {
            calls.incrementAndGet(); return CompletableFuture.failedFuture(new SentimentClientException("401", false, false));
        }).toCompletableFuture().join());
        assertEquals(buffered, breaker.getMetrics().getNumberOfBufferedCalls());
        assertEquals(3, calls.get());
    }

    @Test void open_circuit_and_process_local_concurrency_reject_before_dispatch() {
        var breaker = breaker();
        breaker.transitionToOpenState();
        var calls = new AtomicInteger();
        var rejected = guard(breaker, Duration.ofSeconds(30), 1).execute(() -> {
            calls.incrementAndGet(); return CompletableFuture.completedFuture("unexpected");
        });
        assertInstanceOf(SentimentClientGuard.DispatchDeferredException.class, failure(rejected));
        assertEquals(0, calls.get());

        breaker.transitionToClosedState();
        var guard = guard(breaker, Duration.ofSeconds(30), 1);
        var held = new CompletableFuture<String>();
        guard.execute(() -> held);
        var full = guard.execute(() -> { calls.incrementAndGet(); return CompletableFuture.completedFuture("unexpected"); });
        assertInstanceOf(SentimentClientGuard.DispatchDeferredException.class, failure(full));
        assertEquals(0, calls.get());
        held.complete("done");
    }

    @Test void half_open_stale_reservation_releases_the_permission_as_unused() {
        var breaker = breaker();
        breaker.transitionToOpenState();
        breaker.transitionToHalfOpenState();
        int before = breaker.getMetrics().getNumberOfBufferedCalls();
        var result = guard(breaker, Duration.ofSeconds(30), 1).execute(() ->
                CompletableFuture.failedFuture(new SentimentClientGuard.UnusedPermitException("stale")));
        assertInstanceOf(SentimentClientGuard.UnusedPermitException.class, failure(result));
        assertEquals(before, breaker.getMetrics().getNumberOfBufferedCalls());
    }

    @Test void timeout_counts_once_and_late_completion_cannot_change_circuit_accounting() {
        var breaker = breaker();
        var transport = new CompletableFuture<String>();
        var result = guard(breaker, Duration.ofNanos(1), 1).execute(() -> transport);
        assertInstanceOf(TimeoutException.class, failure(result));
        assertEquals(1, breaker.getMetrics().getNumberOfFailedCalls());
        int buffered = breaker.getMetrics().getNumberOfBufferedCalls();
        transport.complete("late");
        assertEquals(buffered, breaker.getMetrics().getNumberOfBufferedCalls());
        assertEquals(1, breaker.getMetrics().getNumberOfFailedCalls());
    }

    @Test void dispatched_retry_after_response_is_one_failure_and_never_an_internal_retry() throws Exception {
        try(var server=new MockWebServer()){
            server.start();server.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After","120").setBody("{}"));
            var request=new SentimentAnalysisRequest("01K4A000000000000000000001",new NewsId("01K4A000000000000000000002"),"title","content",LanguageCode.ENGLISH,
                    new ContentHash("sha256:"+"a".repeat(64)),new SentimentModelRelease("v1","model","prep","sentiment-v1"));
            var adapter=new HttpSentimentInferenceAdapter(HttpClient.newHttpClient(),server.url("/").uri(),"secret-service-token",new ObjectMapper(),new SentimentContractMapper());
            var breaker=breaker();var result=guard(breaker,Duration.ofSeconds(30),1).execute(()->adapter.analyze(request));
            assertInstanceOf(SentimentClientException.class,failure(result));
            assertEquals(1,server.getRequestCount());assertEquals(1,breaker.getMetrics().getNumberOfFailedCalls());
        }
    }

    private SentimentClientGuard guard(CircuitBreaker breaker, Duration timeout, int concurrency) {
        return new SentimentClientGuard(breaker, TimeLimiter.of(timeout), scheduler, concurrency);
    }
    private static CircuitBreaker breaker() {
        return CircuitBreaker.of("sentiment-test", CircuitBreakerConfig.custom().failureRateThreshold(50)
                .minimumNumberOfCalls(10).slidingWindowSize(10).permittedNumberOfCallsInHalfOpenState(1)
                .waitDurationInOpenState(Duration.ofSeconds(30)).build());
    }
    private static Throwable failure(CompletionStage<?> stage) {
        var thrown = assertThrows(CompletionException.class, () -> stage.toCompletableFuture().join());
        return thrown.getCause();
    }
}

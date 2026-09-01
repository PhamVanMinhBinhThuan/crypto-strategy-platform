package com.cryptostrategy.platform.worker.news.sentiment;

import static org.junit.jupiter.api.Assertions.*;
import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.timelimiter.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;

class SentimentClientGuardTest {
    private ScheduledExecutorService scheduler;
    @BeforeEach void create(){scheduler=Executors.newSingleThreadScheduledExecutor();}
    @AfterEach void close(){scheduler.shutdownNow();}
    private CircuitBreaker breaker(){return CircuitBreaker.of("test",CircuitBreakerConfig.custom().failureRateThreshold(50).minimumNumberOfCalls(1).slidingWindowSize(10).waitDurationInOpenState(Duration.ofSeconds(30)).build());}
    private SentimentClientGuard guard(CircuitBreaker breaker){return new SentimentClientGuard(breaker,TimeLimiter.of(Duration.ofSeconds(30)),scheduler,1);}
    @Test void permanent_4xx_releases_permission_without_circuit_accounting(){
        var breaker=breaker();var calls=new AtomicInteger();
        var stage=guard(breaker).execute(()->{calls.incrementAndGet();return CompletableFuture.failedFuture(new SentimentClientException("bad request",false,false));});
        assertThrows(CompletionException.class,()->stage.toCompletableFuture().join());
        assertEquals(1,calls.get());assertEquals(0,breaker.getMetrics().getNumberOfBufferedCalls());assertEquals(CircuitBreaker.State.CLOSED,breaker.getState());
    }
    @Test void invalid_2xx_counts_one_failure_and_opens_without_retry(){
        var breaker=breaker();var calls=new AtomicInteger();
        var stage=guard(breaker).execute(()->{calls.incrementAndGet();return CompletableFuture.failedFuture(new SentimentClientException("mismatch",false,true));});
        assertThrows(CompletionException.class,()->stage.toCompletableFuture().join());
        assertEquals(1,calls.get());assertEquals(1,breaker.getMetrics().getNumberOfFailedCalls());assertEquals(CircuitBreaker.State.OPEN,breaker.getState());
    }
    @Test void stale_reservation_releases_admitted_permission_as_unused(){
        var breaker=breaker();
        var stage=guard(breaker).execute(()->CompletableFuture.failedFuture(new SentimentClientGuard.UnusedPermitException("stale")));
        assertThrows(CompletionException.class,()->stage.toCompletableFuture().join());
        assertEquals(0,breaker.getMetrics().getNumberOfBufferedCalls());
    }
}

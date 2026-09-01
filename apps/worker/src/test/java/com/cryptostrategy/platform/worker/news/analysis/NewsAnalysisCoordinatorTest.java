package com.cryptostrategy.platform.worker.news.analysis;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.in.NewsAnalysisUseCase;
import com.cryptostrategy.platform.worker.config.NewsWorkerProperties;
import com.cryptostrategy.platform.worker.news.sentiment.SentimentClientGuard;
import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

class NewsAnalysisCoordinatorTest {
    private static final Instant NOW=Instant.parse("2026-09-01T00:00:00Z");
    private static final SentimentModelRelease RELEASE=new SentimentModelRelease("v1","model","prep","sentiment-v1");
    private ScheduledExecutorService scheduler;
    @BeforeEach void setUp(){scheduler=Executors.newSingleThreadScheduledExecutor();}
    @AfterEach void tearDown(){scheduler.shutdownNow();}

    @Test void performs_ordered_claim_reservation_single_dispatch_and_completion(){
        var analysis=new FakeAnalysis(List.of(item(0)));analysis.outcome=CompletableFuture.completedFuture(outcome(analysis.claimed.getFirst()));
        coordinator(analysis,breaker()).poll("worker-1");
        assertEquals(List.of("acquire","start","analyze","complete"),analysis.events);
    }

    @Test void scheduler_completes_readiness_before_any_claim_and_coalesces_overlapping_polls(){
        var analysis=new FakeAnalysis(List.of());var coordinator=coordinator(analysis,breaker());var readiness=new CompletableFuture<Boolean>();
        var scheduled=new NewsAnalysisScheduler(coordinator,()->readiness);
        scheduled.poll();scheduled.poll();
        assertTrue(analysis.events.isEmpty());
        readiness.complete(true);
        assertEquals(List.of("acquire"),analysis.events);
    }

    @Test void open_circuit_defers_without_consuming_an_attempt(){
        var analysis=new FakeAnalysis(List.of(item(0)));var breaker=breaker();breaker.transitionToOpenState();
        coordinator(analysis,breaker).poll("worker-1");
        assertEquals(List.of("acquire","defer"),analysis.events);
    }

    @Test void reservation_to_transport_crash_window_leaves_the_reserved_attempt_consumed(){
        var analysis=new FakeAnalysis(List.of(item(0)));analysis.outcome=new CompletableFuture<>();
        coordinator(analysis,breaker()).poll("worker-1");
        assertEquals(List.of("acquire","start","analyze"),analysis.events);
        assertEquals(1,analysis.reservations);
        assertFalse(analysis.events.contains("complete"));
        assertFalse(analysis.events.contains("fail"));
    }

    @Test void stale_start_releases_the_circuit_permit_and_late_or_failed_completion_is_not_retried(){
        var stale=new FakeAnalysis(List.of(item(0)));stale.startAllowed=false;
        var staleBreaker=breaker();coordinator(stale,staleBreaker).poll("worker-1");
        assertEquals(List.of("acquire","start"),stale.events);
        assertEquals(0,staleBreaker.getMetrics().getNumberOfBufferedCalls());

        var persistenceFailure=new FakeAnalysis(List.of(item(0)));persistenceFailure.failCompletion=true;
        persistenceFailure.outcome=CompletableFuture.completedFuture(outcome(persistenceFailure.claimed.getFirst()));
        coordinator(persistenceFailure,breaker()).poll("worker-1");
        assertEquals(List.of("acquire","start","analyze","complete"),persistenceFailure.events);
        assertFalse(persistenceFailure.events.contains("fail"));
    }

    private NewsAnalysisCoordinator coordinator(FakeAnalysis analysis,CircuitBreaker breaker){
        var guard=new SentimentClientGuard(breaker,TimeLimiter.of(Duration.ofSeconds(30)),scheduler,1);
        var policy=new NewsWorkerProperties.Analysis(Duration.ofSeconds(120),25,3,List.of(Duration.ofSeconds(5),Duration.ofSeconds(30)));
        return new NewsAnalysisCoordinator(analysis,guard,policy,RELEASE,Clock.fixed(NOW,ZoneOffset.UTC),
                new NewsAnalysisObservability(new SimpleMeterRegistry()));
    }
    private static CircuitBreaker breaker(){return CircuitBreaker.of("coordinator",CircuitBreakerConfig.custom().minimumNumberOfCalls(10).slidingWindowSize(10).waitDurationInOpenState(Duration.ofSeconds(30)).build());}
    private static NewsItem item(int attempts){
        var id=NewsId.generate();var hash=new ContentHash("sha256:"+"a".repeat(64));
        var lease=new AnalysisLease("worker-1",com.cryptostrategy.platform.domain.api.identity.Ulids.generate(),NOW.plusSeconds(120),attempts,"v1");
        return new NewsItem(id,"title","content",NOW,NOW,hash,AnalysisStatus.ANALYZING,new NewsSource("fixture"),
                new CanonicalNewsUrl("https://example.test/"+id.value()),LanguageCode.ENGLISH,Optional.empty(),Optional.of("v1"),Optional.of(lease),Optional.empty(),attempts,List.of());
    }
    private static SentimentAnalysisOutcome outcome(NewsItem item){return new SentimentAnalysisOutcome(
            com.cryptostrategy.platform.domain.api.identity.Ulids.generate(),item.newsId(),LanguageCode.ENGLISH,item.contentHash(),RELEASE,
            SentimentLabel.POSITIVE,new BigDecimal("0.8"),new BigDecimal("0.6"),NOW);}

    private static final class FakeAnalysis implements NewsAnalysisUseCase{
        private final List<NewsItem> claimed;private final List<String> events=new ArrayList<>();
        private CompletionStage<SentimentAnalysisOutcome> outcome;private boolean startAllowed=true;private boolean failCompletion;private int reservations;
        private FakeAnalysis(List<NewsItem> claimed){this.claimed=claimed;}
        @Override public List<NewsItem> acquire(Acquire command){events.add("acquire");return claimed;}
        @Override public boolean startAttempt(StartAttempt command){events.add("start");if(startAllowed)reservations++;return startAllowed;}
        @Override public CompletionStage<SentimentAnalysisOutcome> analyze(SentimentAnalysisRequest request){events.add("analyze");
            return outcome.thenApply(value->new SentimentAnalysisOutcome(request.requestId(),value.newsId(),value.language(),value.contentHash(),value.release(),value.label(),value.confidence(),value.polarityScore(),value.analyzedAt()));}
        @Override public void complete(Complete command){events.add("complete");if(failCompletion)throw new IllegalStateException("stale/persistence failure");}
        @Override public void defer(Defer command){events.add("defer");}
        @Override public void fail(Fail command){events.add("fail");}
    }
}

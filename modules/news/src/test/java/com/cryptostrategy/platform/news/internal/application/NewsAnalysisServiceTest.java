package com.cryptostrategy.platform.news.internal.application;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.news.api.NewsModuleFactory.AnalysisPolicy;
import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.in.NewsAnalysisUseCase.*;
import com.cryptostrategy.platform.news.api.port.out.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class NewsAnalysisServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-01T00:00:00Z");
    private static final NewsId ID = NewsId.generate();
    private static final ContentHash HASH = new ContentHash("sha256:" + "a".repeat(64));

    @Test void delegates_claim_and_reserves_the_attempt_before_dispatch() {
        var work = new RecordingWork();
        var service = service(work);
        assertEquals(List.of(), service.acquire(new Acquire("worker-1", NOW, Duration.ofSeconds(120), 10)));
        assertTrue(service.startAttempt(new StartAttempt(ID, "lease", HASH, "v1")));
        assertEquals(List.of("claim", "reserve"), work.events);
    }

    @Test void fake_clock_drives_deferral_and_bounded_five_thirty_second_retry_policy() {
        var work = new RecordingWork();
        var service = service(work);
        service.defer(new Defer(ID, "lease", Duration.ofSeconds(5)));
        assertEquals(NOW.plusSeconds(5), work.eligibleAt);

        service.fail(new Fail(ID, "lease", true, 1));
        assertTrue(work.retryable);
        assertEquals(NOW.plusSeconds(5), work.eligibleAt);
        service.fail(new Fail(ID, "lease", true, 2));
        assertTrue(work.retryable);
        assertEquals(NOW.plusSeconds(30), work.eligibleAt);
        service.fail(new Fail(ID, "lease", true, 3));
        assertFalse(work.retryable);
        service.fail(new Fail(ID, "lease", false, 1));
        assertFalse(work.retryable);
    }

    private static NewsAnalysisService service(RecordingWork work) {
        return new NewsAnalysisService(work, request -> CompletableFuture.failedFuture(new AssertionError("not called")),
                Clock.fixed(NOW, ZoneOffset.UTC),
                new AnalysisPolicy(Duration.ofSeconds(120), 10, 3, List.of(Duration.ofSeconds(5), Duration.ofSeconds(30))));
    }

    private static final class RecordingWork implements AnalysisWorkStore {
        private final List<String> events = new ArrayList<>();
        private Instant eligibleAt;
        private boolean retryable;
        @Override public List<NewsItem> claim(String owner, Instant now, Duration duration, int limit) { events.add("claim"); return List.of(); }
        @Override public boolean reserveAttempt(NewsId id, String token, ContentHash hash, String version) { events.add("reserve"); return true; }
        @Override public void defer(NewsId id, String token, Instant eligible) { eligibleAt = eligible; }
        @Override public void fail(NewsId id, String token, boolean retryable, Instant eligible) { this.retryable = retryable; eligibleAt = eligible; }
        @Override public void complete(NewsId id, String token, SentimentResult result) { events.add("complete"); }
    }
}

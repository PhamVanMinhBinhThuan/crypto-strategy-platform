package com.cryptostrategy.platform.news.api;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import com.cryptostrategy.platform.news.api.port.out.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NewsModuleFactoryTest {
    private static final Instant NOW = Instant.parse("2026-09-01T01:02:03Z");
    private static final ContentHash HASH = new ContentHash("sha256:" + "a".repeat(64));
    private static final SentimentModelRelease RELEASE =
            new SentimentModelRelease("1.0.0", "multichannel-english", "multichannel-whitespace-en-1", "sentiment-v1");

    @Test
    void composes_use_cases_from_injected_boundaries_and_registers_the_active_release() {
        var registered = new AtomicReference<SentimentModelRelease>();
        var activated = new AtomicReference<String>();
        var saved = new AtomicReference<NewsItem>();
        NewsProvider provider = new NewsProvider() {
            @Override public NewsSource source() { return new NewsSource("fixture"); }
            @Override public List<ProviderNewsItem> fetchSince(Instant since) {
                return List.of(new ProviderNewsItem("https://raw.invalid/1", "raw", "raw", "en", NOW,
                        Optional.empty(), List.of()));
            }
        };
        NewsNormalizationPolicy normalization = (url, title, content, language) ->
                new NewsNormalizationPolicy.NormalizedNews(new CanonicalNewsUrl("https://example.test/news"),
                        "normalized title", "normalized content", LanguageCode.ENGLISH, HASH);

        var dependencies = new NewsModuleFactory.Dependencies(
                List.of(provider), item -> { saved.set(item); return NewsItemStore.SaveOutcome.INSERTED; },
                noOpWork(), query -> new ListNewsUseCase.Page(List.of(), Optional.empty()),
                newsId -> Optional.empty(), symbols -> Map.of(),
                request -> CompletableFuture.failedFuture(new AssertionError("not invoked")), new SentimentModelReleaseStore() {
                    @Override public void registerOrVerify(SentimentModelRelease release){registered.set(release);}
                    @Override public void activateForEnglish(String modelVersion){activated.set(modelVersion);}
                },
                normalization, Clock.fixed(NOW, ZoneOffset.UTC));
        var components = NewsModuleFactory.create(dependencies, settings());

        assertSame(RELEASE, registered.get());
        assertEquals(RELEASE.modelVersion(), activated.get());
        assertNotNull(components.analysis());
        assertNotNull(components.queries());
        assertNotNull(components.audit());
        assertEquals(CollectNewsStatus.ACCEPTED, collect(components).status());
        assertEquals(NOW, saved.get().crawledAt());
        assertEquals("normalized title", saved.get().title());
        assertEquals(Optional.of(RELEASE.modelVersion()), saved.get().targetModelVersion());
    }

    @Test
    void analysis_retry_policy_uses_the_injected_clock_and_configured_attempt_budget() {
        var failure = new AtomicReference<Failure>();
        AnalysisWorkStore work = new AnalysisWorkStore() {
            @Override public List<NewsItem> claim(String owner, Instant now, Duration duration, int limit) { return List.of(); }
            @Override public boolean reserveAttempt(NewsId id, String token, ContentHash hash, String version) { return true; }
            @Override public void defer(NewsId id, String token, Instant eligible) { }
            @Override public void fail(NewsId id, String token, boolean retryable, Instant eligible) {
                failure.set(new Failure(retryable, eligible));
            }
            @Override public void complete(NewsId id, String token, SentimentResult result) { }
        };
        var dependencies = new NewsModuleFactory.Dependencies(
                List.of(), item -> NewsItemStore.SaveOutcome.INSERTED, work,
                query -> new ListNewsUseCase.Page(List.of(), Optional.empty()), newsId -> Optional.empty(),
                symbols -> Map.of(), request -> CompletableFuture.completedFuture(null), release -> { },
                (url, title, content, language) -> { throw new AssertionError("not invoked"); },
                Clock.fixed(NOW, ZoneOffset.UTC));
        var analysis = NewsModuleFactory.create(dependencies, settings()).analysis();
        var id = NewsId.generate();

        analysis.fail(new com.cryptostrategy.platform.news.api.port.in.NewsAnalysisUseCase.Fail(id, "lease", true, 1));
        assertEquals(new Failure(true, NOW.plusSeconds(5)), failure.get());
        analysis.fail(new com.cryptostrategy.platform.news.api.port.in.NewsAnalysisUseCase.Fail(id, "lease", true, 3));
        assertEquals(new Failure(false, NOW.plusSeconds(30)), failure.get());
    }

    private static CollectionResult collect(NewsModuleFactory.Components components) {
        var outcome = components.collection().collectSince(Instant.EPOCH).getFirst();
        return new CollectionResult(CollectNewsStatus.valueOf(outcome.status().name()));
    }

    private static NewsModuleFactory.Settings settings() {
        return new NewsModuleFactory.Settings(RELEASE,
                new NewsModuleFactory.AnalysisPolicy(Duration.ofSeconds(120), 10, 3,
                        List.of(Duration.ofSeconds(5), Duration.ofSeconds(30))));
    }

    private static AnalysisWorkStore noOpWork() {
        return new AnalysisWorkStore() {
            @Override public List<NewsItem> claim(String owner, Instant now, Duration duration, int limit) { return List.of(); }
            @Override public boolean reserveAttempt(NewsId id, String token, ContentHash hash, String version) { return false; }
            @Override public void defer(NewsId id, String token, Instant eligible) { }
            @Override public void fail(NewsId id, String token, boolean retryable, Instant eligible) { }
            @Override public void complete(NewsId id, String token, SentimentResult result) { }
        };
    }

    private enum CollectNewsStatus { ACCEPTED }
    private record CollectionResult(CollectNewsStatus status) {}
    private record Failure(boolean retryable, Instant eligibleAt) {}
}

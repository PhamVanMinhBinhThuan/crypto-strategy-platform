package com.cryptostrategy.platform.news.internal.application;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.in.CollectNewsUseCase.Status;
import com.cryptostrategy.platform.news.api.port.out.*;
import com.cryptostrategy.platform.news.internal.normalization.CanonicalNewsNormalizer;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NewsCollectionServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-01T00:00:00Z");

    @Test void isolates_provider_failures_and_accepts_other_providers_without_sentiment_availability() {
        NewsProvider broken = provider("broken", () -> { throw new IllegalStateException("offline"); });
        NewsProvider healthy = provider("healthy", () -> List.of(item("https://example.test/ok", "en", List.of())));
        var saved = new AtomicInteger();
        var service = service(List.of(broken, healthy), news -> { saved.incrementAndGet(); return NewsItemStore.SaveOutcome.INSERTED; }, symbols -> Map.of());

        var outcomes = service.collectSince(Instant.EPOCH);

        assertEquals(List.of(Status.PROVIDER_FAILED, Status.ACCEPTED), outcomes.stream().map(o -> o.status()).toList());
        assertEquals(1, saved.get());
    }

    @Test void sanitizes_maps_assets_and_keeps_non_english_news_visible_without_an_analysis_target() {
        var bitcoin = AssetId.generate();
        var captured = new ArrayList<NewsItem>();
        var provider = provider("feed", () -> List.of(
                new ProviderNewsItem("https://example.test/en?utm_source=x", "<b>Bullish</b>",
                        "<script>bad()</script><p>Market rises</p>", "en", NOW, Optional.of("source-1"), List.of("BTC", "UNKNOWN")),
                item("https://example.test/fr", "fr", List.of())));
        var service = service(List.of(provider), news -> { captured.add(news); return NewsItemStore.SaveOutcome.INSERTED; },
                symbols -> symbols.contains("BTC") ? Map.of("BTC", bitcoin) : Map.of());

        assertEquals(List.of(Status.ACCEPTED, Status.ACCEPTED),
                service.collectSince(Instant.EPOCH).stream().map(o -> o.status()).toList());
        assertEquals("Bullish", captured.getFirst().title());
        assertFalse(captured.getFirst().content().contains("bad"));
        assertEquals(List.of(bitcoin), captured.getFirst().relatedAssets().stream().map(RelatedNewsAsset::assetId).toList());
        assertEquals(Optional.of("model-1"), captured.getFirst().targetModelVersion());
        assertEquals(Optional.empty(), captured.getLast().targetModelVersion());
        assertEquals(AnalysisStatus.PENDING, captured.getLast().analysisStatus());
    }

    @Test void reports_duplicate_content_conflict_and_invalid_language_as_stable_outcomes() {
        var provider = provider("feed", () -> List.of(
                item("https://example.test/duplicate", "en", List.of()),
                item("https://example.test/conflict", "en", List.of()),
                item("https://example.test/invalid", "not_a_language", List.of())));
        NewsItemStore store = news -> news.url().toString().endsWith("duplicate")
                ? NewsItemStore.SaveOutcome.ALREADY_PRESENT : NewsItemStore.SaveOutcome.CONFLICT;

        var outcomes = service(List.of(provider), store, symbols -> Map.of()).collectSince(Instant.EPOCH);

        assertEquals(List.of(Status.DUPLICATE, Status.REJECTED, Status.REJECTED),
                outcomes.stream().map(o -> o.status()).toList());
        assertEquals("ALREADY_PRESENT", outcomes.getFirst().reason());
        assertEquals("CONFLICT", outcomes.get(1).reason());
        assertEquals("IllegalArgumentException", outcomes.getLast().reason());
    }

    private static NewsCollectionService service(List<NewsProvider> providers, NewsItemStore store, AssetResolver assets) {
        return new NewsCollectionService(providers, store, assets, new CanonicalNewsNormalizer(),
                Clock.fixed(NOW, ZoneOffset.UTC), Optional.of("model-1"));
    }

    private static ProviderNewsItem item(String url, String language, List<String> symbols) {
        return new ProviderNewsItem(url, "Title", "Content", language, NOW, Optional.empty(), symbols);
    }

    private static NewsProvider provider(String source, Items items) {
        return new NewsProvider() {
            @Override public NewsSource source() { return new NewsSource(source); }
            @Override public List<ProviderNewsItem> fetchSince(Instant since) { return items.get(); }
        };
    }

    @FunctionalInterface private interface Items { List<ProviderNewsItem> get(); }
}

package com.cryptostrategy.platform.news.provider;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.news.api.model.ProviderNewsItem;
import com.cryptostrategy.platform.news.api.port.out.NewsProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Reusable behavioral contract for every configured NewsProvider adapter. */
public abstract class NewsProviderContract {
    protected static final Instant EARLY = Instant.parse("2026-08-30T00:00:00Z");
    protected static final Instant LATE = Instant.parse("2026-08-31T00:00:00Z");

    protected abstract NewsProvider provider(List<ProviderNewsItem> items);
    protected abstract NewsProvider malformedProvider();
    protected abstract NewsProvider timeoutProvider();

    @Test void returns_valid_items_at_or_after_the_requested_boundary() {
        var old = item("https://example.test/old", "old", EARLY.minusSeconds(1));
        var current = item("https://example.test/current", "current", EARLY);
        assertEquals(List.of(current), provider(List.of(old, current)).fetchSince(EARLY));
    }

    @Test void preserves_duplicates_reordered_input_and_conflicting_url_content_for_domain_deduplication() {
        var first = item("https://example.test/same", "first", LATE);
        var conflict = item("https://example.test/same", "changed", EARLY);
        var duplicate = item("https://example.test/same", "first", LATE);
        assertEquals(List.of(conflict, first, duplicate), provider(List.of(conflict, first, duplicate)).fetchSince(EARLY));
    }

    @Test void translates_malformed_and_timeout_failures_without_returning_partial_data() {
        assertThrows(RuntimeException.class, () -> malformedProvider().fetchSince(EARLY));
        assertThrows(RuntimeException.class, () -> timeoutProvider().fetchSince(EARLY));
    }

    protected static ProviderNewsItem item(String url, String content, Instant publishedAt) {
        return new ProviderNewsItem(url, "Title", content, "en", publishedAt, Optional.empty(), List.of("BTC"));
    }
}

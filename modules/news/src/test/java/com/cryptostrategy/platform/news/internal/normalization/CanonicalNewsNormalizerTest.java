package com.cryptostrategy.platform.news.internal.normalization;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CanonicalNewsNormalizerTest {
    private final CanonicalNewsNormalizer normalizer = new CanonicalNewsNormalizer();

    @Test void sanitizes_normalizes_url_and_matches_the_news_canonical_v1_golden_hash() {
        var first = normalizer.normalize("HTTPS://Example.COM:443/a/../news?utm_source=x&b=2&a=1#fragment",
                " <b>Good</b>  news ", "<script>steal()</script><p>Fresh&nbsp; market</p><p>update</p>", "EN");
        var second = normalizer.normalize("https://example.com/news?a=1&b=2", "Good news", "Fresh market\n\nupdate", "en");
        assertEquals("https://example.com/news?a=1&b=2", first.url().toString());
        assertFalse(first.content().contains("steal"));
        assertEquals("Good news", first.title());
        assertEquals("Fresh market\n\nupdate", first.content());
        assertEquals(first.contentHash(), second.contentHash());
        assertEquals("sha256:914c025415d96faf5f37e0ed02b29d95040e89d3935889eccd7708a2387a95dd",
                first.contentHash().value());
    }

    @Test void normalizes_unicode_controls_and_whitespace_before_hashing() {
        var decomposed = normalizer.normalize("https://example.test/unicode", "Cafe\u0301\t market",
                "first\u0000  line\r\n\r\n\r\n second\u00a0line", "en");
        var composed = normalizer.normalize("https://example.test/unicode", "Caf\u00e9 market",
                "first line\n\nsecond line", "en");
        assertEquals("Caf\u00e9 market", decomposed.title());
        assertEquals("first line\n\nsecond line", decomposed.content());
        assertEquals(composed.contentHash(), decomposed.contentHash());
    }

    @Test void rejects_non_http_urls_and_empty_or_oversized_fields() {
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalize("file:///tmp/x", "title", "body", "en"));
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalize("https://example.test", "title", "<script>x</script>", "en"));
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalize("https://example.test", "x".repeat(1001), "body", "en"));
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalize("https://example.test", "title", "x".repeat(100001), "en"));
    }
}

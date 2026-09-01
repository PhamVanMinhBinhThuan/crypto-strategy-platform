package com.cryptostrategy.platform.news.api.model;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NewsDomainModelTest {
    private static final ContentHash HASH = new ContentHash("sha256:" + "a".repeat(64));
    private static final SentimentModelRelease RELEASE = new SentimentModelRelease("v1", "model", "prep", "sentiment-v1");

    @Test void validates_hash_language_release_and_exact_score_ranges(){
        assertThrows(IllegalArgumentException.class,()->new ContentHash("abc"));
        assertEquals("en",new LanguageCode("EN").value());
        assertThrows(IllegalArgumentException.class, () -> new NewsId("lowercase00000000000000000"));
        assertThrows(IllegalArgumentException.class, () -> new CanonicalNewsUrl("ftp://example.test/news"));
        assertThrows(IllegalArgumentException.class, () -> new SentimentModelRelease("v1", " ", "prep", "sentiment-v1"));
        assertThrows(IllegalArgumentException.class,()->new SentimentResult(SentimentResultId.generate(),NewsId.generate(),
            HASH,LanguageCode.ENGLISH,RELEASE,SentimentLabel.POSITIVE,
            new BigDecimal("1.0000000001"),BigDecimal.ZERO,Instant.EPOCH));
        assertThrows(IllegalArgumentException.class,()->new SentimentResult(SentimentResultId.generate(),NewsId.generate(),
            HASH,LanguageCode.ENGLISH,RELEASE,SentimentLabel.POSITIVE,
            new BigDecimal("0.12345678901"),BigDecimal.ZERO,Instant.EPOCH));
    }

    @Test void enforces_news_state_lease_target_and_time_invariants() {
        var lease = new AnalysisLease("worker-1", "30000000000000000000000001", Instant.parse("2026-09-01T00:02:00Z"), 1, "v1");
        assertDoesNotThrow(() -> item(AnalysisStatus.ANALYZING, Optional.of(lease), Optional.empty(), Optional.of("v1"), 1));
        assertThrows(IllegalArgumentException.class, () -> item(AnalysisStatus.PENDING, Optional.of(lease), Optional.empty(), Optional.of("v1"), 1));
        assertThrows(IllegalArgumentException.class, () -> item(AnalysisStatus.FAILED_RETRYABLE, Optional.empty(), Optional.empty(), Optional.of("v1"), 1));
        assertThrows(IllegalArgumentException.class, () -> item(AnalysisStatus.ANALYZING, Optional.of(lease), Optional.empty(), Optional.of("other"), 1));
        assertThrows(IllegalArgumentException.class, () -> item(AnalysisStatus.ANALYZING, Optional.of(lease), Optional.empty(), Optional.of("v1"), 0));
        assertThrows(IllegalArgumentException.class, () -> new NewsItem(NewsId.generate(), "title", "content",
            Instant.parse("2026-09-01T00:01:00Z"), Instant.EPOCH, HASH, AnalysisStatus.PENDING,
            new NewsSource("fixture"), new CanonicalNewsUrl("https://example.test/news"), LanguageCode.ENGLISH,
            Optional.empty(), Optional.of("v1"), Optional.empty(), Optional.empty(), 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> item(AnalysisStatus.PENDING, Optional.empty(), Optional.empty(), Optional.empty(), 0));
    }

    @Test void collapses_duplicate_related_assets_and_keeps_results_immutable_values() {
        var asset = new AssetId("40000000000000000000000001");
        var news = new NewsItem(NewsId.generate(), "title", "content", Instant.EPOCH, Instant.EPOCH, HASH,
            AnalysisStatus.PENDING, new NewsSource("fixture"), new CanonicalNewsUrl("https://example.test/news"),
            LanguageCode.ENGLISH, Optional.empty(), Optional.of("v1"), Optional.empty(), Optional.empty(), 0,
            List.of(new RelatedNewsAsset(asset, Optional.empty()), new RelatedNewsAsset(asset, Optional.of(BigDecimal.ONE))));
        assertEquals(1, news.relatedAssets().size());
        assertThrows(UnsupportedOperationException.class, () -> news.relatedAssets().add(new RelatedNewsAsset(AssetId.generate(), Optional.empty())));
    }

    private static NewsItem item(AnalysisStatus status, Optional<AnalysisLease> lease, Optional<Instant> next,
                                 Optional<String> target, int attempts) {
        return new NewsItem(NewsId.generate(), "title", "content", Instant.EPOCH, Instant.EPOCH, HASH, status,
            new NewsSource("fixture"), new CanonicalNewsUrl("https://example.test/news"), LanguageCode.ENGLISH,
            Optional.empty(), target, lease, next, attempts, List.of());
    }
}

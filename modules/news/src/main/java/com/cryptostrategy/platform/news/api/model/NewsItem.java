package com.cryptostrategy.platform.news.api.model;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record NewsItem(NewsId newsId, String title, String content, Instant publishedAt, Instant crawledAt,
                       ContentHash contentHash, AnalysisStatus analysisStatus, NewsSource source,
                       CanonicalNewsUrl url, LanguageCode language, Optional<String> sourceItemId,
                       Optional<String> targetModelVersion, Optional<AnalysisLease> lease,
                       Optional<Instant> nextEligibleAttempt, int attemptCount, List<RelatedNewsAsset> relatedAssets) {
    public NewsItem {
        Objects.requireNonNull(newsId, "newsId");
        title = required(title, "title", 1000);
        content = required(content, "content", 100000);
        Objects.requireNonNull(publishedAt, "publishedAt");
        Objects.requireNonNull(crawledAt, "crawledAt");
        Objects.requireNonNull(contentHash, "contentHash");
        Objects.requireNonNull(analysisStatus, "analysisStatus");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(language, "language");
        sourceItemId = nullableText(sourceItemId);
        targetModelVersion = nullableText(targetModelVersion);
        lease = lease == null ? Optional.empty() : lease;
        nextEligibleAttempt = nextEligibleAttempt == null ? Optional.empty() : nextEligibleAttempt;
        relatedAssets = collapseAssets(relatedAssets);
        if (attemptCount < 0) throw new IllegalArgumentException("Attempt count cannot be negative");
        if (crawledAt.isBefore(publishedAt)) throw new IllegalArgumentException("News cannot be crawled before publication");
        if (LanguageCode.ENGLISH.equals(language) && targetModelVersion.isEmpty())
            throw new IllegalArgumentException("English News requires a target model release");
        if ((analysisStatus == AnalysisStatus.ANALYZING) != lease.isPresent())
            throw new IllegalArgumentException("Only ANALYZING News must have a lease");
        if ((analysisStatus == AnalysisStatus.FAILED_RETRYABLE) != nextEligibleAttempt.isPresent())
            throw new IllegalArgumentException("Only FAILED_RETRYABLE News must have next eligibility");
        if (lease.isPresent()) {
            var value = lease.orElseThrow();
            if (value.attemptCount() != attemptCount)
                throw new IllegalArgumentException("Lease attempt count must match News");
            if (targetModelVersion.filter(value.targetModelVersion()::equals).isEmpty())
                throw new IllegalArgumentException("Lease target must match News target release");
        }
    }
    private static String required(String value, String name, int max) {
        value = Objects.requireNonNull(value, name).trim();
        if (value.isEmpty() || value.length() > max) throw new IllegalArgumentException("Invalid " + name);
        return value;
    }
    private static Optional<String> nullableText(Optional<String> value) {
        return value == null ? Optional.empty() : value.map(String::trim).filter(v -> !v.isEmpty());
    }

    private static List<RelatedNewsAsset> collapseAssets(List<RelatedNewsAsset> values) {
        var unique = new LinkedHashMap<com.cryptostrategy.platform.domain.api.market.AssetId, RelatedNewsAsset>();
        for (var value : values == null ? List.<RelatedNewsAsset>of() : values) {
            Objects.requireNonNull(value, "relatedAsset");
            unique.merge(value.assetId(), value, NewsItem::moreRelevant);
        }
        return List.copyOf(unique.values());
    }

    private static RelatedNewsAsset moreRelevant(RelatedNewsAsset first, RelatedNewsAsset second) {
        BigDecimal left = first.relevanceScore().orElse(BigDecimal.ZERO);
        BigDecimal right = second.relevanceScore().orElse(BigDecimal.ZERO);
        return right.compareTo(left) > 0 ? second : first;
    }
}

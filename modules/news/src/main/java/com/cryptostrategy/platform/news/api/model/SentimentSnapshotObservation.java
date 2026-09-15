package com.cryptostrategy.platform.news.api.model;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record SentimentSnapshotObservation(
        NewsId newsId,
        SentimentResultId sentimentResultId,
        AssetId assetId,
        Instant publishedAt,
        Instant analyzedAt,
        ContentHash contentHash,
        BigDecimal polarityScore,
        BigDecimal confidence
) implements Comparable<SentimentSnapshotObservation> {
    public SentimentSnapshotObservation {
        Objects.requireNonNull(newsId); Objects.requireNonNull(sentimentResultId);
        Objects.requireNonNull(assetId); Objects.requireNonNull(publishedAt);
        Objects.requireNonNull(analyzedAt); Objects.requireNonNull(contentHash);
        polarityScore = range(polarityScore, BigDecimal.ONE.negate(), BigDecimal.ONE, "polarityScore");
        confidence = range(confidence, BigDecimal.ZERO, BigDecimal.ONE, "confidence");
    }

    @Override public int compareTo(SentimentSnapshotObservation other) {
        int byTime = publishedAt.compareTo(other.publishedAt);
        return byTime != 0 ? byTime : newsId.value().compareTo(other.newsId.value());
    }

    private static BigDecimal range(BigDecimal value, BigDecimal minimum, BigDecimal maximum,
            String name) {
        value = Objects.requireNonNull(value, name);
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0)
            throw new IllegalArgumentException(name + " is outside its valid range");
        return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
    }
}

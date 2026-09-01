package com.cryptostrategy.platform.news.api.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record SentimentResult(SentimentResultId resultId, NewsId newsId, ContentHash contentHash,
                              LanguageCode language, SentimentModelRelease release, SentimentLabel label,
                              BigDecimal confidence, BigDecimal polarityScore, Instant analyzedAt) {
    public SentimentResult {
        Objects.requireNonNull(resultId, "resultId"); Objects.requireNonNull(newsId, "newsId");
        Objects.requireNonNull(contentHash, "contentHash"); Objects.requireNonNull(language, "language");
        Objects.requireNonNull(release, "release"); Objects.requireNonNull(label, "label");
        range(confidence, BigDecimal.ZERO, BigDecimal.ONE, "confidence");
        range(polarityScore, BigDecimal.ONE.negate(), BigDecimal.ONE, "polarityScore");
        Objects.requireNonNull(analyzedAt, "analyzedAt");
    }
    private static void range(BigDecimal value, BigDecimal min, BigDecimal max, String name) {
        Objects.requireNonNull(value, name);
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) throw new IllegalArgumentException(name + " is out of range");
        if (value.stripTrailingZeros().scale() > 10) throw new IllegalArgumentException(name + " exceeds scale 10");
    }
}

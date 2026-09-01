package com.cryptostrategy.platform.news.api.model;

import java.math.BigDecimal;
import java.time.Instant;

public record SentimentAnalysisOutcome(String requestId, NewsId newsId, LanguageCode language,
        ContentHash contentHash, SentimentModelRelease release, SentimentLabel label,
        BigDecimal confidence, BigDecimal polarityScore, Instant analyzedAt) {
    public SentimentResult toResult(SentimentResultId id) {
        return new SentimentResult(id, newsId, contentHash, language, release, label, confidence, polarityScore, analyzedAt);
    }
}

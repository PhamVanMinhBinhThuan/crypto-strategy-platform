package com.cryptostrategy.platform.news.api.model;

import java.util.Objects;

public record SentimentAnalysisRequest(String requestId, NewsId newsId, String title, String content,
                                       LanguageCode language, ContentHash contentHash, SentimentModelRelease release) {
    public SentimentAnalysisRequest {
        requestId = com.cryptostrategy.platform.domain.api.identity.Ulids.requireValid(requestId);
        Objects.requireNonNull(newsId); Objects.requireNonNull(title); Objects.requireNonNull(content);
        Objects.requireNonNull(language); Objects.requireNonNull(contentHash); Objects.requireNonNull(release);
    }
}

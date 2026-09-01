package com.cryptostrategy.platform.news.api.model;

import java.util.Objects;

public record SentimentAnalysisRequest(SentimentRequestId requestId, NewsId newsId, String title, String content,
                                       LanguageCode language, ContentHash contentHash, SentimentModelRelease release) {
    public SentimentAnalysisRequest(String requestId, NewsId newsId, String title, String content,
                                    LanguageCode language, ContentHash contentHash, SentimentModelRelease release) {
        this(new SentimentRequestId(requestId), newsId, title, content, language, contentHash, release);
    }
    public SentimentAnalysisRequest {
        Objects.requireNonNull(requestId); Objects.requireNonNull(newsId); Objects.requireNonNull(title); Objects.requireNonNull(content);
        Objects.requireNonNull(language); Objects.requireNonNull(contentHash); Objects.requireNonNull(release);
    }
}

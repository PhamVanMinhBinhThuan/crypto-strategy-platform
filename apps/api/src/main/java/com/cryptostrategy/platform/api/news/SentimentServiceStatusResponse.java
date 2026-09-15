package com.cryptostrategy.platform.api.news;

import java.time.Instant;

public record SentimentServiceStatusResponse(
        Status status,
        String message,
        Instant checkedAt) {

    public enum Status {
        AVAILABLE,
        DEGRADED
    }

    public static SentimentServiceStatusResponse available() {
        return new SentimentServiceStatusResponse(
                Status.AVAILABLE,
                "Sentiment analysis is available.",
                Instant.now());
    }

    public static SentimentServiceStatusResponse degraded() {
        return new SentimentServiceStatusResponse(
                Status.DEGRADED,
                "Sentiment service is unavailable. Existing results remain available; new items will be analyzed after recovery.",
                Instant.now());
    }
}

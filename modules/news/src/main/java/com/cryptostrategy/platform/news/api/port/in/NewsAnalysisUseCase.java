package com.cryptostrategy.platform.news.api.port.in;

import com.cryptostrategy.platform.news.api.model.ContentHash;
import com.cryptostrategy.platform.news.api.model.NewsId;
import com.cryptostrategy.platform.news.api.model.NewsItem;
import com.cryptostrategy.platform.news.api.model.SentimentResult;
import com.cryptostrategy.platform.news.api.model.SentimentAnalysisOutcome;
import com.cryptostrategy.platform.news.api.model.SentimentAnalysisRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Application boundary for the durable News analysis lifecycle. */
public interface NewsAnalysisUseCase {
    List<NewsItem> acquire(Acquire command);
    boolean startAttempt(StartAttempt command);
    CompletionStage<SentimentAnalysisOutcome> analyze(SentimentAnalysisRequest request);
    void complete(Complete command);
    void defer(Defer command);
    void fail(Fail command);

    record Acquire(String owner, Instant now, Duration leaseDuration, int limit) {
        public Acquire {
            owner = required(owner, "owner");
            Objects.requireNonNull(now, "now");
            Objects.requireNonNull(leaseDuration, "leaseDuration");
            if (leaseDuration.isZero() || leaseDuration.isNegative()) throw new IllegalArgumentException("leaseDuration must be positive");
            if (limit < 1 || limit > 25) throw new IllegalArgumentException("limit must be between 1 and 25");
        }
    }

    record StartAttempt(NewsId newsId, String leaseToken, ContentHash contentHash, String modelVersion) {
        public StartAttempt {
            Objects.requireNonNull(newsId, "newsId");
            leaseToken = required(leaseToken, "leaseToken");
            Objects.requireNonNull(contentHash, "contentHash");
            modelVersion = required(modelVersion, "modelVersion");
        }
    }

    record Complete(NewsId newsId, String leaseToken, SentimentResult result) {
        public Complete {
            Objects.requireNonNull(newsId, "newsId");
            leaseToken = required(leaseToken, "leaseToken");
            Objects.requireNonNull(result, "result");
            if (!newsId.equals(result.newsId())) throw new IllegalArgumentException("Result News identity mismatch");
        }
    }

    record Defer(NewsId newsId, String leaseToken, Duration delay) {
        public Defer {
            Objects.requireNonNull(newsId, "newsId");
            leaseToken = required(leaseToken, "leaseToken");
            Objects.requireNonNull(delay, "delay");
            if (delay.isNegative()) throw new IllegalArgumentException("delay cannot be negative");
        }
    }

    record Fail(NewsId newsId, String leaseToken, boolean transientFailure, int consumedAttempts) {
        public Fail {
            Objects.requireNonNull(newsId, "newsId");
            leaseToken = required(leaseToken, "leaseToken");
            if (consumedAttempts < 1) throw new IllegalArgumentException("consumedAttempts must be positive");
        }
    }

    private static String required(String value, String field) {
        value = Objects.requireNonNull(value, field).trim();
        if (value.isEmpty()) throw new IllegalArgumentException(field + " is required");
        return value;
    }
}

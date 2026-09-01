package com.cryptostrategy.platform.news.internal.validation;

import com.cryptostrategy.platform.news.api.error.NewsErrorCode;
import com.cryptostrategy.platform.news.api.error.NewsException;
import com.cryptostrategy.platform.news.api.model.SentimentAnalysisOutcome;
import com.cryptostrategy.platform.news.api.model.SentimentAnalysisRequest;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/** Performs semantic validation after transport/schema decoding and before persistence. */
public final class SentimentResponseValidator {
    public SentimentAnalysisOutcome validate(SentimentAnalysisRequest request, SentimentAnalysisOutcome outcome) {
        Objects.requireNonNull(request, "request");
        if (outcome == null) throw invalid("response");
        require(request.requestId().equals(outcome.requestId()), "requestId");
        require(request.newsId().equals(outcome.newsId()), "newsId");
        require(request.language().equals(outcome.language()), "language");
        require(request.contentHash().equals(outcome.contentHash()), "contentHash");
        require(request.release().equals(outcome.release()), "release");
        require(outcome.label() != null, "label");
        requireDecimal(outcome.confidence(), BigDecimal.ZERO, BigDecimal.ONE, "confidence");
        requireDecimal(outcome.polarityScore(), BigDecimal.ONE.negate(), BigDecimal.ONE, "polarityScore");
        require(outcome.analyzedAt() != null, "analyzedAt");
        return outcome;
    }

    private static void requireDecimal(BigDecimal value, BigDecimal minimum, BigDecimal maximum, String field) {
        require(value != null && value.compareTo(minimum) >= 0 && value.compareTo(maximum) <= 0
                && Math.max(0, value.stripTrailingZeros().scale()) <= 10, field);
    }

    private static void require(boolean condition, String field) {
        if (!condition) throw invalid(field);
    }

    private static NewsException invalid(String field) {
        return new NewsException(NewsErrorCode.INVALID_SENTIMENT_RESPONSE, "Invalid sentiment response",
                Map.of("field", field), null);
    }
}

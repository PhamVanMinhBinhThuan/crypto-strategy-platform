package com.cryptostrategy.platform.news.internal.validation;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.news.api.error.*;
import com.cryptostrategy.platform.news.api.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SentimentResponseValidatorTest {
    private static final NewsId NEWS_ID = NewsId.generate();
    private static final ContentHash HASH = new ContentHash("sha256:" + "a".repeat(64));
    private static final SentimentModelRelease RELEASE = new SentimentModelRelease("v1", "model", "prep", "sentiment-v1");
    private static final SentimentAnalysisRequest REQUEST = new SentimentAnalysisRequest(
            com.cryptostrategy.platform.domain.api.identity.Ulids.generate(), NEWS_ID, "title", "content", LanguageCode.ENGLISH, HASH, RELEASE);
    private final SentimentResponseValidator validator = new SentimentResponseValidator();

    @Test void accepts_equivalent_duplicate_outcomes() {
        var first = valid();
        var duplicate = valid();
        assertEquals(first, validator.validate(REQUEST, first));
        assertEquals(first, validator.validate(REQUEST, duplicate));
    }

    @Test void rejects_every_echo_or_release_mismatch() {
        assertInvalid(outcome(SentimentRequestId.generate(), NEWS_ID, LanguageCode.ENGLISH, HASH, RELEASE,
                SentimentLabel.POSITIVE, new BigDecimal("0.8"), new BigDecimal("0.6"), Instant.EPOCH));
        assertInvalid(outcome(REQUEST.requestId(), NewsId.generate(), LanguageCode.ENGLISH, HASH, RELEASE,
                SentimentLabel.POSITIVE, new BigDecimal("0.8"), new BigDecimal("0.6"), Instant.EPOCH));
        assertInvalid(outcome(REQUEST.requestId(), NEWS_ID, new LanguageCode("fr"), HASH, RELEASE,
                SentimentLabel.POSITIVE, new BigDecimal("0.8"), new BigDecimal("0.6"), Instant.EPOCH));
        assertInvalid(outcome(REQUEST.requestId(), NEWS_ID, LanguageCode.ENGLISH, new ContentHash("sha256:" + "b".repeat(64)), RELEASE,
                SentimentLabel.POSITIVE, new BigDecimal("0.8"), new BigDecimal("0.6"), Instant.EPOCH));
        assertInvalid(outcome(REQUEST.requestId(), NEWS_ID, LanguageCode.ENGLISH, HASH,
                new SentimentModelRelease("v2", "model", "prep", "sentiment-v1"), SentimentLabel.POSITIVE,
                new BigDecimal("0.8"), new BigDecimal("0.6"), Instant.EPOCH));
    }

    @Test void rejects_invalid_label_decimal_range_scale_and_time() {
        assertInvalid(outcome(REQUEST.requestId(), NEWS_ID, LanguageCode.ENGLISH, HASH, RELEASE, null,
                new BigDecimal("0.8"), new BigDecimal("0.6"), Instant.EPOCH));
        assertInvalid(outcome(REQUEST.requestId(), NEWS_ID, LanguageCode.ENGLISH, HASH, RELEASE, SentimentLabel.POSITIVE,
                new BigDecimal("1.1"), new BigDecimal("0.6"), Instant.EPOCH));
        assertInvalid(outcome(REQUEST.requestId(), NEWS_ID, LanguageCode.ENGLISH, HASH, RELEASE, SentimentLabel.POSITIVE,
                new BigDecimal("0.12345678901"), new BigDecimal("0.6"), Instant.EPOCH));
        assertInvalid(outcome(REQUEST.requestId(), NEWS_ID, LanguageCode.ENGLISH, HASH, RELEASE, SentimentLabel.POSITIVE,
                new BigDecimal("0.8"), new BigDecimal("-1.1"), Instant.EPOCH));
        assertInvalid(outcome(REQUEST.requestId(), NEWS_ID, LanguageCode.ENGLISH, HASH, RELEASE, SentimentLabel.POSITIVE,
                new BigDecimal("0.8"), new BigDecimal("0.6"), null));
    }

    private SentimentAnalysisOutcome valid() {
        return outcome(REQUEST.requestId(), NEWS_ID, LanguageCode.ENGLISH, HASH, RELEASE, SentimentLabel.POSITIVE,
                new BigDecimal("0.8"), new BigDecimal("0.6"), Instant.EPOCH);
    }
    private void assertInvalid(SentimentAnalysisOutcome outcome) {
        var error = assertThrows(NewsException.class, () -> validator.validate(REQUEST, outcome));
        assertEquals(NewsErrorCode.INVALID_SENTIMENT_RESPONSE, error.code());
    }
    private static SentimentAnalysisOutcome outcome(SentimentRequestId requestId, NewsId newsId, LanguageCode language,
            ContentHash hash, SentimentModelRelease release, SentimentLabel label, BigDecimal confidence,
            BigDecimal polarity, Instant analyzedAt) {
        return new SentimentAnalysisOutcome(requestId, newsId, language, hash, release, label, confidence, polarity, analyzedAt);
    }
}

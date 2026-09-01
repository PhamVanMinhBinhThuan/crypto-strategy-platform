package com.cryptostrategy.platform.news.api.model;

import java.math.BigDecimal;
import java.time.Instant;

public record SentimentAuditRecord(SentimentResultId resultId,NewsId newsId,LanguageCode language,ContentHash contentHash,
        SentimentModelRelease release,SentimentLabel label,BigDecimal confidence,BigDecimal polarityScore,Instant analyzedAt) {}

package com.cryptostrategy.platform.contracts.sentiment.v1;

import java.time.Instant;

public record SentimentAnalyzeSuccess(String requestId,String newsId,String language,String contentHash,
        String contractVersion,String modelName,String modelVersion,String preprocessingVersion,String label,
        String confidence,String polarityScore,Instant analyzedAt) {}

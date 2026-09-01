package com.cryptostrategy.platform.contracts.sentiment.v1;

public record SentimentAnalyzeRequest(String requestId,String newsId,String title,String content,String language,
        String contentHash,String contractVersion,String modelName,String modelVersion,String preprocessingVersion) {}

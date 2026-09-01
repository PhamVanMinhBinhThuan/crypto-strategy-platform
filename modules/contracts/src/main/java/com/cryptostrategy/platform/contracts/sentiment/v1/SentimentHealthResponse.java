package com.cryptostrategy.platform.contracts.sentiment.v1;

import java.util.Optional;

public record SentimentHealthResponse(String status,Optional<String> contractVersion,Optional<String> modelVersion) {
    public SentimentHealthResponse { contractVersion=contractVersion==null?Optional.empty():contractVersion; modelVersion=modelVersion==null?Optional.empty():modelVersion; }
}

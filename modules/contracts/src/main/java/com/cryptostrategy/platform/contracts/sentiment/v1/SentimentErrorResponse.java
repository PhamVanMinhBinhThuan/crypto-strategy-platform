package com.cryptostrategy.platform.contracts.sentiment.v1;

import java.util.Optional;

public record SentimentErrorResponse(Optional<String> requestId,String code,String message,boolean retryable) {
    public SentimentErrorResponse { requestId=requestId==null?Optional.empty():requestId; }
}

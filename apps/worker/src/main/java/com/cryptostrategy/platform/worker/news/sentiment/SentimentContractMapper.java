package com.cryptostrategy.platform.worker.news.sentiment;

import com.cryptostrategy.platform.contracts.sentiment.v1.*;
import com.cryptostrategy.platform.news.api.model.*;
import java.math.BigDecimal;

public final class SentimentContractMapper {
    public SentimentAnalyzeRequest toWire(SentimentAnalysisRequest request) {
        var release=request.release();
        return new SentimentAnalyzeRequest(request.requestId(),request.newsId().value(),request.title(),request.content(),request.language().value(),request.contentHash().value(),release.contractVersion(),release.modelName(),release.modelVersion(),release.preprocessingVersion());
    }
    public SentimentAnalysisOutcome fromWire(SentimentAnalysisRequest request,SentimentAnalyzeSuccess wire) {
        var r=request.release();
        if(!request.requestId().equals(wire.requestId())||!request.newsId().value().equals(wire.newsId())||!request.language().value().equals(wire.language())||!request.contentHash().value().equals(wire.contentHash())||
           !r.contractVersion().equals(wire.contractVersion())||!r.modelName().equals(wire.modelName())||!r.modelVersion().equals(wire.modelVersion())||!r.preprocessingVersion().equals(wire.preprocessingVersion()))
            throw new SentimentClientException("Response provenance mismatch",false,true);
        try {
            BigDecimal confidence=new BigDecimal(wire.confidence()),polarity=new BigDecimal(wire.polarityScore());
            if(confidence.scale()>10||polarity.scale()>10||confidence.compareTo(BigDecimal.ZERO)<0||confidence.compareTo(BigDecimal.ONE)>0||polarity.compareTo(BigDecimal.ONE.negate())<0||polarity.compareTo(BigDecimal.ONE)>0)
                throw new IllegalArgumentException();
            return new SentimentAnalysisOutcome(wire.requestId(),request.newsId(),request.language(),request.contentHash(),r,SentimentLabel.valueOf(wire.label()),confidence,polarity,wire.analyzedAt());
        } catch(RuntimeException error) { throw new SentimentClientException("Invalid sentiment response",false,true,error); }
    }
}

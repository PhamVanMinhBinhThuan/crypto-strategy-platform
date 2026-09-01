package com.cryptostrategy.platform.news.api.port.out;

import com.cryptostrategy.platform.news.api.model.SentimentAnalysisOutcome;
import com.cryptostrategy.platform.news.api.model.SentimentAnalysisRequest;
import java.util.concurrent.CompletionStage;

public interface SentimentInferencePort {
    CompletionStage<SentimentAnalysisOutcome> analyze(SentimentAnalysisRequest request);
}

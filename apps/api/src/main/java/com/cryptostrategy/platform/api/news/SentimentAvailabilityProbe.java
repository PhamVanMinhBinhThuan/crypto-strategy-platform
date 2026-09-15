package com.cryptostrategy.platform.api.news;

import java.util.concurrent.CompletionStage;

public interface SentimentAvailabilityProbe {
    CompletionStage<Boolean> check();
}

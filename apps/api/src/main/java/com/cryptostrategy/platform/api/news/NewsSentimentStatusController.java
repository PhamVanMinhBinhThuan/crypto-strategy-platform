package com.cryptostrategy.platform.api.news;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news-items/sentiment-status")
public final class NewsSentimentStatusController {
    private final SentimentAvailabilityProbe sentiment;

    public NewsSentimentStatusController(SentimentAvailabilityProbe sentiment) {
        this.sentiment = Objects.requireNonNull(sentiment, "sentiment");
    }

    @GetMapping
    public CompletionStage<SentimentServiceStatusResponse> getStatus() {
        return sentiment.check().thenApply(available -> available
                ? SentimentServiceStatusResponse.available()
                : SentimentServiceStatusResponse.degraded());
    }
}

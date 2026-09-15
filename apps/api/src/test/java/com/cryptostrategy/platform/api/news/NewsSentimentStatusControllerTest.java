package com.cryptostrategy.platform.api.news;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class NewsSentimentStatusControllerTest {
    @Test
    void reportsAvailableWhenSentimentIsReady() {
        var controller = new NewsSentimentStatusController(
                () -> CompletableFuture.completedFuture(true));

        var response = controller.getStatus().toCompletableFuture().join();

        assertEquals(SentimentServiceStatusResponse.Status.AVAILABLE, response.status());
        assertNotNull(response.checkedAt());
    }

    @Test
    void reportsDegradedWithoutHidingExistingResults() {
        var controller = new NewsSentimentStatusController(
                () -> CompletableFuture.completedFuture(false));

        var response = controller.getStatus().toCompletableFuture().join();

        assertEquals(SentimentServiceStatusResponse.Status.DEGRADED, response.status());
        assertEquals(
                "Sentiment service is unavailable. Existing results remain available; new items will be analyzed after recovery.",
                response.message());
    }
}

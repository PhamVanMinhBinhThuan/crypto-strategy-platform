package com.cryptostrategy.platform.api.news;

import com.fasterxml.jackson.annotation.JsonValue;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record NewsResponse(
        List<Item> items,
        Optional<String> nextCursor,
        boolean hasMore) {
    public NewsResponse {
        items = List.copyOf(items);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }
    public record Item(
            NewsResponseId newsId,
            String title,
            String source,
            String url,
            Instant publishedAt,
            String analysisStatus,
            List<String> relatedAssetIds,
            Optional<Sentiment> sentiment) {
        public Item {
            relatedAssetIds = List.copyOf(relatedAssetIds);
            sentiment = sentiment == null ? Optional.empty() : sentiment;
        }
    }
    public record Sentiment(String label, String confidence, String polarityScore) {}

    public record NewsResponseId(@JsonValue String value) {}
}

package com.cryptostrategy.platform.api.news;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonValue;

public record NewsResponse(List<Item> items,Optional<String> nextCursor,boolean hasMore) {
    public record Item(NewsResponseId newsId,String title,String source,String url,Instant publishedAt,String analysisStatus,
                       List<String> relatedAssetIds,Optional<Sentiment> sentiment) {}
    public record Sentiment(String label,String confidence,String polarityScore) {}
    public record NewsResponseId(@JsonValue String value) {}
}

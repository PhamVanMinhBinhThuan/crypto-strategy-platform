package com.cryptostrategy.platform.api.news;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record NewsResponse(List<Item> items,Optional<String> nextCursor,boolean hasMore) {
    public record Item(String newsId,String title,String source,String url,Instant publishedAt,String analysisStatus,
                       List<String> relatedAssetIds,Optional<Sentiment> sentiment) {}
    public record Sentiment(String label,String confidence,String polarityScore) {}
}

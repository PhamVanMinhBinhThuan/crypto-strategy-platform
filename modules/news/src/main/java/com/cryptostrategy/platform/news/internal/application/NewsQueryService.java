package com.cryptostrategy.platform.news.internal.application;

import com.cryptostrategy.platform.news.api.model.AnalysisStatus;
import com.cryptostrategy.platform.news.api.model.NewsCursor;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import com.cryptostrategy.platform.news.api.port.out.NewsQueryPort;
import java.util.Objects;

public final class NewsQueryService implements ListNewsUseCase {
    private final NewsQueryPort queries;
    public NewsQueryService(NewsQueryPort queries) { this.queries = Objects.requireNonNull(queries); }
    @Override public Page list(Query query) {
        Objects.requireNonNull(query, "query");
        if (query.limit() < 1 || query.limit() > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        query.cursor().ifPresent(NewsCursor::decode);
        var page=Objects.requireNonNull(queries.list(query),"query result");
        var sanitized=page.items().stream().map(item->item.analysisStatus()==AnalysisStatus.ANALYZED?item:
                new Item(item.newsId(),item.title(),item.source(),item.url(),item.publishedAt(),item.analysisStatus(),
                        item.relatedAssetIds(),java.util.Optional.empty(),java.util.Optional.empty(),java.util.Optional.empty())).toList();
        for(int index=1;index<sanitized.size();index++){
            var previous=sanitized.get(index-1);var current=sanitized.get(index);
            int byTime=previous.publishedAt().compareTo(current.publishedAt());
            if(byTime<0||(byTime==0&&previous.newsId().value().compareTo(current.newsId().value())<0))
                throw new IllegalStateException("News query port returned unstable ordering");
        }
        page.nextCursor().ifPresent(NewsCursor::decode);
        return new Page(sanitized,page.nextCursor());
    }
}

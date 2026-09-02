package com.cryptostrategy.platform.api.news;

import com.cryptostrategy.platform.news.api.model.AnalysisStatus;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news-items")
public final class NewsController {
    private final ListNewsUseCase news;
    private final NewsQueryMapper queries;

    public NewsController(ListNewsUseCase news, NewsQueryMapper queries) {
        this.news = Objects.requireNonNull(news, "news");
        this.queries = Objects.requireNonNull(queries, "queries");
    }

    @GetMapping
    public NewsResponse list(
            @RequestParam(required = false) String tradingPairId,
            @RequestParam(required = false) Set<AnalysisStatus> analysisStatus,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        var page = news.list(queries.map(tradingPairId, analysisStatus, cursor, limit));
        var items = page.items().stream().map(item -> {
            Optional<NewsResponse.Sentiment> sentiment = item.analysisStatus() == AnalysisStatus.ANALYZED
                    && item.label().isPresent()
                    && item.confidence().isPresent()
                    && item.polarityScore().isPresent()
                    ? Optional.of(new NewsResponse.Sentiment(
                            item.label().orElseThrow(),
                            item.confidence().orElseThrow().toPlainString(),
                            item.polarityScore().orElseThrow().toPlainString()))
                    : Optional.empty();
            return new NewsResponse.Item(
                    new NewsResponse.NewsResponseId(item.newsId().value()),
                    item.title(),
                    item.source(),
                    item.url(),
                    item.publishedAt(),
                    item.analysisStatus().name(),
                    item.relatedAssetIds().stream().map(asset -> asset.value()).toList(),
                    sentiment);
        }).toList();
        return new NewsResponse(items, page.nextCursor(), page.nextCursor().isPresent());
    }
}

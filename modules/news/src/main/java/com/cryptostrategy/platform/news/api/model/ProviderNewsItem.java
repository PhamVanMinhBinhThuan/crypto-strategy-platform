package com.cryptostrategy.platform.news.api.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record ProviderNewsItem(String url, String titleHtml, String contentHtml, String language,
        Instant publishedAt, Optional<String> sourceItemId, List<String> assetSymbols) {
    public ProviderNewsItem {
        sourceItemId = sourceItemId == null ? Optional.empty() : sourceItemId;
        assetSymbols = List.copyOf(assetSymbols == null ? List.of() : assetSymbols);
    }
}

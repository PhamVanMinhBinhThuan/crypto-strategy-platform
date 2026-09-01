package com.cryptostrategy.platform.news.api.model;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record RelatedNewsAsset(AssetId assetId, Optional<BigDecimal> relevanceScore) {
    public RelatedNewsAsset {
        Objects.requireNonNull(assetId, "assetId");
        relevanceScore = relevanceScore == null ? Optional.empty() : relevanceScore;
        relevanceScore.ifPresent(score -> {
            if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.ONE) > 0)
                throw new IllegalArgumentException("Relevance score must be between 0 and 1");
        });
    }
}

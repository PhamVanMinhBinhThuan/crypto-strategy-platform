package com.cryptostrategy.platform.api.news;

import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.marketdata.api.port.in.GetTradingPairUseCase;
import com.cryptostrategy.platform.news.api.model.AnalysisStatus;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public final class NewsQueryMapper {
    private final GetTradingPairUseCase pairs;
    private final PageRequestMapper pages;

    public NewsQueryMapper(GetTradingPairUseCase pairs, PageRequestMapper pages) {
        this.pairs = Objects.requireNonNull(pairs, "pairs");
        this.pages = Objects.requireNonNull(pages, "pages");
    }

    ListNewsUseCase.Query map(
            String tradingPairId,
            Set<AnalysisStatus> statuses,
            String cursor,
            Integer limit) {
        Set<AssetId> assets = new LinkedHashSet<>();
        if (tradingPairId != null) {
            var pair = pairs.getTradingPair(new TradingPairId(tradingPairId))
                    .orElseThrow(() -> new IllegalArgumentException("Trading pair is invalid"));
            assets.add(pair.baseAsset().assetId());
            assets.add(pair.quoteAsset().assetId());
        }
        var page = pages.map(limit, cursor, 20, 100);
        return new ListNewsUseCase.Query(
                assets,
                statuses == null ? Set.of() : Set.copyOf(statuses),
                page.cursor(),
                page.limit());
    }
}

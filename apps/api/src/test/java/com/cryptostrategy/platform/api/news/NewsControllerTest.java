package com.cryptostrategy.platform.api.news;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.domain.api.market.Asset;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.marketdata.api.port.in.GetTradingPairUseCase;
import com.cryptostrategy.platform.news.api.model.AnalysisStatus;
import com.cryptostrategy.platform.news.api.model.NewsId;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NewsControllerTest {
    @Test
    void resolvesBothPairAssetsAndExposesOnlyLightweightSentiment() {
        var base = new Asset(
                new AssetId("10000000000000000000000001"),
                new AssetSymbol("BTC"), Optional.empty(), true);
        var quote = new Asset(
                new AssetId("10000000000000000000000002"),
                new AssetSymbol("USDT"), Optional.empty(), true);
        var pairId = new TradingPairId("10000000000000000000000003");
        GetTradingPairUseCase pairs = mock(GetTradingPairUseCase.class);
        when(pairs.getTradingPair(pairId))
                .thenReturn(Optional.of(new TradingPair(pairId, base, quote, true)));
        var captured = new AtomicReference<ListNewsUseCase.Query>();
        ListNewsUseCase useCase = query -> {
            captured.set(query);
            return new ListNewsUseCase.Page(List.of(new ListNewsUseCase.Item(
                    new NewsId("20000000000000000000000001"),
                    "Tin", "fixture", "https://example.test/news", Instant.EPOCH,
                    AnalysisStatus.ANALYZED,
                    List.of(base.assetId(), quote.assetId()),
                    Optional.of("POSITIVE"),
                    Optional.of(new BigDecimal("0.82")),
                    Optional.of(new BigDecimal("0.64")))), Optional.empty());
        };
        var controller = new NewsController(
                useCase, new NewsQueryMapper(pairs, new PageRequestMapper()));

        var response = controller.list(pairId.value(), Set.of(), null, 20);

        assertNotNull(response);
        assertEquals(Set.of(base.assetId(), quote.assetId()), captured.get().eitherAsset());
        assertEquals("0.82", response.items().getFirst().sentiment().orElseThrow().confidence());
        assertFalse(Arrays.stream(NewsResponse.Item.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase())
                .anyMatch(name -> name.contains("model") || name.contains("hash") || name.contains("lease")));
    }

    @Test
    void exposesEveryAnalysisStateWithoutFabricatingSentimentOrProvenance() {
        GetTradingPairUseCase pairs = mock(GetTradingPairUseCase.class);
        ListNewsUseCase useCase = query -> {
            var items = Arrays.stream(AnalysisStatus.values())
                    .map(status -> new ListNewsUseCase.Item(
                            NewsId.generate(),
                            "English title",
                            "fixture",
                            "https://example.test/" + status,
                            Instant.EPOCH,
                            status,
                            List.of(),
                            status == AnalysisStatus.ANALYZED
                                    ? Optional.of("NEGATIVE") : Optional.empty(),
                            status == AnalysisStatus.ANALYZED
                                    ? Optional.of(new BigDecimal("0.9")) : Optional.empty(),
                            status == AnalysisStatus.ANALYZED
                                    ? Optional.of(new BigDecimal("-0.8")) : Optional.empty()))
                    .toList();
            return new ListNewsUseCase.Page(items, Optional.of("opaque-cursor"));
        };
        var controller = new NewsController(
                useCase, new NewsQueryMapper(pairs, new PageRequestMapper()));

        var response = controller.list(null, Set.of(), null, 20);

        assertNotNull(response);
        assertEquals(AnalysisStatus.values().length, response.items().size());
        assertEquals(1, response.items().stream()
                .filter(item -> item.sentiment().isPresent()).count());
        assertEquals(Optional.of("opaque-cursor"), response.nextCursor());
        assertTrue(response.hasMore());
        assertFalse(Arrays.stream(NewsResponse.Sentiment.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase())
                .anyMatch(name -> name.contains("model")
                        || name.contains("version")
                        || name.contains("hash")));
    }
}

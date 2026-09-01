package com.cryptostrategy.platform.api.news;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cryptostrategy.platform.domain.api.market.*;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketReferenceDataStore;
import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NewsControllerTest {
    @Test void resolves_both_pair_assets_and_exposes_only_lightweight_sentiment(){
        var base=new Asset(new AssetId("10000000000000000000000001"),new AssetSymbol("BTC"),Optional.empty(),true);
        var quote=new Asset(new AssetId("10000000000000000000000002"),new AssetSymbol("USDT"),Optional.empty(),true);
        var pairId=new TradingPairId("10000000000000000000000003");var references=mock(MarketReferenceDataStore.class);
        when(references.findTradingPair(pairId)).thenReturn(Optional.of(new TradingPair(pairId,base,quote,true)));
        var captured=new AtomicReference<ListNewsUseCase.Query>();
        ListNewsUseCase useCase=query->{captured.set(query);return new ListNewsUseCase.Page(List.of(new ListNewsUseCase.Item(
            new NewsId("20000000000000000000000001"),"Tin","fixture","https://example.test/news",Instant.EPOCH,AnalysisStatus.ANALYZED,
            List.of(base.assetId(),quote.assetId()),Optional.of("POSITIVE"),Optional.of(new BigDecimal("0.82")),Optional.of(new BigDecimal("0.64")))),Optional.empty());};
        var response=new NewsController(useCase,references).list(pairId.value(),Set.of(),null,20).getBody();
        assertNotNull(response);assertEquals(Set.of(base.assetId(),quote.assetId()),captured.get().eitherAsset());
        assertEquals("0.82",response.items().getFirst().sentiment().orElseThrow().confidence());
        assertFalse(Arrays.stream(NewsResponse.Item.class.getRecordComponents()).map(c->c.getName().toLowerCase()).anyMatch(name->name.contains("model")||name.contains("hash")||name.contains("lease")));
    }

    @Test void exposes_every_analysis_state_without_fabricating_sentiment_or_provenance(){
        var references=mock(MarketReferenceDataStore.class);
        ListNewsUseCase useCase=query->{
            var items=Arrays.stream(AnalysisStatus.values()).map(status->new ListNewsUseCase.Item(
                    NewsId.generate(),"English title","fixture","https://example.test/"+status,Instant.EPOCH,status,List.of(),
                    status==AnalysisStatus.ANALYZED?Optional.of("NEGATIVE"):Optional.empty(),
                    status==AnalysisStatus.ANALYZED?Optional.of(new BigDecimal("0.9")):Optional.empty(),
                    status==AnalysisStatus.ANALYZED?Optional.of(new BigDecimal("-0.8")):Optional.empty())).toList();
            return new ListNewsUseCase.Page(items,Optional.of("opaque-cursor"));};
        var body=new NewsController(useCase,references).list(null,Set.of(),null,20).getBody();
        assertNotNull(body);assertEquals(AnalysisStatus.values().length,body.items().size());
        assertEquals(1,body.items().stream().filter(item->item.sentiment().isPresent()).count());
        assertEquals(Optional.of("opaque-cursor"),body.nextCursor());
        assertTrue(body.hasMore());
        assertFalse(Arrays.stream(NewsResponse.Sentiment.class.getRecordComponents()).map(c->c.getName().toLowerCase())
                .anyMatch(name->name.contains("model")||name.contains("version")||name.contains("hash")));
    }
}

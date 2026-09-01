package com.cryptostrategy.platform.api.news;

import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketReferenceDataStore;
import com.cryptostrategy.platform.news.api.model.AnalysisStatus;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/news-items")
public class NewsController {
    private final ListNewsUseCase news; private final MarketReferenceDataStore pairs;
    public NewsController(ListNewsUseCase news,MarketReferenceDataStore pairs){this.news=news;this.pairs=pairs;}
    @GetMapping public ResponseEntity<NewsResponse> list(@RequestParam(required=false)String tradingPairId,
            @RequestParam(required=false)Set<AnalysisStatus> analysisStatus,@RequestParam(required=false)String cursor,
            @RequestParam(defaultValue="20")int limit){
        var assets=new LinkedHashSet<com.cryptostrategy.platform.domain.api.market.AssetId>();
        if(tradingPairId!=null){var pair=pairs.findTradingPair(new TradingPairId(tradingPairId));if(pair.isEmpty())return ResponseEntity.notFound().build();assets.add(pair.get().baseAsset().assetId());assets.add(pair.get().quoteAsset().assetId());}
        var page=news.list(new ListNewsUseCase.Query(assets,analysisStatus==null?Set.of():Set.copyOf(analysisStatus),Optional.ofNullable(cursor),limit));
        var items=page.items().stream().map(i->{
            Optional<NewsResponse.Sentiment> sentiment=i.analysisStatus()==AnalysisStatus.ANALYZED&&i.label().isPresent()&&i.confidence().isPresent()&&i.polarityScore().isPresent()
                ?Optional.of(new NewsResponse.Sentiment(i.label().orElseThrow(),i.confidence().orElseThrow().toPlainString(),i.polarityScore().orElseThrow().toPlainString())):Optional.empty();
            return new NewsResponse.Item(new NewsResponse.NewsResponseId(i.newsId().value()),i.title(),i.source(),i.url(),i.publishedAt(),i.analysisStatus().name(),i.relatedAssetIds().stream().map(a->a.value()).toList(),sentiment);
        }).toList();
        return ResponseEntity.ok(new NewsResponse(items,page.nextCursor(),page.nextCursor().isPresent()));
    }
}

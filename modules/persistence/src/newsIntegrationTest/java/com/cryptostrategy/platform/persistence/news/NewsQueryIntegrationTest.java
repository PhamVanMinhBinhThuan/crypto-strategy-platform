package com.cryptostrategy.platform.persistence.news;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class NewsQueryIntegrationTest extends PostgresNewsTestSupport {
    @Test void either_asset_filter_deduplicates_dual_links_and_keyset_paginates_with_status_projection(){
        var release=release();var base=new AssetId(insertAsset());var quote=new AssetId(insertAsset());
        var both=List.of(new RelatedNewsAsset(base,Optional.empty()),new RelatedNewsAsset(quote,Optional.empty()));
        var first=item(uniqueUrl(),"first",release,both);var second=item(uniqueUrl(),"second",release,List.of(new RelatedNewsAsset(quote,Optional.empty())));
        persistence.items().saveIfAbsent(first);persistence.items().saveIfAbsent(second);
        var page=persistence.queries().list(new ListNewsUseCase.Query(Set.of(base,quote),Set.of(AnalysisStatus.PENDING),Optional.empty(),1));
        assertEquals(1,page.items().size());assertTrue(page.nextCursor().isPresent());assertEquals(AnalysisStatus.PENDING,page.items().getFirst().analysisStatus());
        assertTrue(page.items().getFirst().confidence().isEmpty());
        var next=persistence.queries().list(new ListNewsUseCase.Query(Set.of(base,quote),Set.of(),page.nextCursor(),1));
        assertEquals(1,next.items().size());assertNotEquals(page.items().getFirst().newsId(),next.items().getFirst().newsId());
    }

    @Test void analyzed_projection_preserves_exact_decimals_and_lightweight_status(){
        var release=release();var item=item(uniqueUrl(),"analyzed",release,List.of());persistence.items().saveIfAbsent(item);
        var now=Instant.parse("2026-09-01T00:00:01Z");var claim=persistence.work().claim("worker",now,Duration.ofSeconds(120),1).getFirst();var token=claim.lease().orElseThrow().token();persistence.work().reserveAttempt(item.newsId(),token,item.contentHash(),release.modelVersion());
        persistence.work().complete(item.newsId(),token,new SentimentResult(SentimentResultId.generate(),item.newsId(),item.contentHash(),LanguageCode.ENGLISH,release,SentimentLabel.POSITIVE,new BigDecimal("0.8200000000"),new BigDecimal("0.6400000000"),now));
        var page=persistence.queries().list(new ListNewsUseCase.Query(Set.of(),Set.of(AnalysisStatus.ANALYZED),Optional.empty(),100));
        var projected=page.items().stream().filter(value->value.newsId().equals(item.newsId())).findFirst().orElseThrow();
        assertEquals("0.8200000000",projected.confidence().orElseThrow().toPlainString());assertEquals("0.6400000000",projected.polarityScore().orElseThrow().toPlainString());
    }
}

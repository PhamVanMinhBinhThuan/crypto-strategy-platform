package com.cryptostrategy.platform.persistence.news;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.out.NewsItemStore;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class NewsCollectionPersistenceIntegrationTest extends PostgresNewsTestSupport {
    @Test void concurrent_canonical_url_insert_is_idempotent_pending_and_associations_are_unique() throws Exception {
        var release=release();String url=uniqueUrl();String assetId=insertAsset();
        var related=List.of(new RelatedNewsAsset(new AssetId(assetId),Optional.of(new BigDecimal("0.9"))));
        var first=item(url,"same",release,related);var second=item(url,"same",release,related);
        var ready=new CountDownLatch(2);var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{
            Callable<NewsItemStore.SaveOutcome> left=()->{ready.countDown();start.await();return persistence.items().saveIfAbsent(first);};
            Callable<NewsItemStore.SaveOutcome> right=()->{ready.countDown();start.await();return persistence.items().saveIfAbsent(second);};
            var a=pool.submit(left);var b=pool.submit(right);ready.await();start.countDown();
            assertEquals(Set.of(NewsItemStore.SaveOutcome.INSERTED,NewsItemStore.SaveOutcome.ALREADY_PRESENT),Set.of(a.get(),b.get()));
        }finally{pool.shutdownNow();}
        var row=jdbc.queryForMap("select source,analysis_status from news.news_item where url=?",url);
        assertEquals("integration",row.get("source"));assertEquals("PENDING",row.get("analysis_status"));
        assertEquals(1,jdbc.queryForObject("select count(*) from news.news_item_asset a join news.news_item n using(news_item_id) where n.url=? and a.asset_id=?",Integer.class,url,assetId));
    }

    @Test void conflicting_content_or_source_identity_never_overwrites_the_accepted_row(){
        var release=release();String url=uniqueUrl();var accepted=item(url,"accepted",release,List.of());
        assertEquals(NewsItemStore.SaveOutcome.INSERTED,persistence.items().saveIfAbsent(accepted));
        assertEquals(NewsItemStore.SaveOutcome.CONFLICT,persistence.items().saveIfAbsent(item(url,"changed",release,List.of())));
        assertEquals(accepted.contentHash().value(),jdbc.queryForObject("select content_hash from news.news_item where url=?",String.class,url));
        String sourceIdentity="provider-"+com.cryptostrategy.platform.domain.api.identity.Ulids.generate();
        var sourceAccepted=withSourceItemId(item(uniqueUrl(),"source-a",release,List.of()),sourceIdentity);
        persistence.items().saveIfAbsent(sourceAccepted);
        assertThrows(RuntimeException.class,()->persistence.items().saveIfAbsent(withSourceItemId(item(uniqueUrl(),"source-b",release,List.of()),sourceIdentity)));
        assertEquals(1,jdbc.queryForObject("select count(*) from news.news_item where source=? and source_item_id=?",Integer.class,"integration",sourceIdentity));
    }
}

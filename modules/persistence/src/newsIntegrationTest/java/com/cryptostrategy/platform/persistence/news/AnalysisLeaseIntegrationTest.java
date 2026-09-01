package com.cryptostrategy.platform.persistence.news;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.news.api.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class AnalysisLeaseIntegrationTest extends PostgresNewsTestSupport {
    @Test void skip_locked_claims_are_exclusive_and_expired_work_is_reclaimed_with_a_new_fence() throws Exception {
        var release=release();var item=item(uniqueUrl(),"lease",release,List.of());persistence.items().saveIfAbsent(item);
        var now=Instant.parse("2026-09-01T00:00:01Z");var ready=new CountDownLatch(2);var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        List<NewsItem> one,two;
        try{
            var a=pool.submit(()->{ready.countDown();start.await();return persistence.work().claim("worker-a",now,Duration.ofSeconds(120),1);});
            var b=pool.submit(()->{ready.countDown();start.await();return persistence.work().claim("worker-b",now,Duration.ofSeconds(120),1);});
            ready.await();start.countDown();one=a.get();two=b.get();
        }finally{pool.shutdownNow();}
        assertEquals(1,one.size()+two.size());var original=one.isEmpty()?two.getFirst():one.getFirst();
        var reclaimed=persistence.work().claim("worker-c",now.plusSeconds(121),Duration.ofSeconds(120),1).getFirst();
        assertEquals(original.newsId(),reclaimed.newsId());assertNotEquals(original.lease().orElseThrow().token(),reclaimed.lease().orElseThrow().token());
        var result=new SentimentResult(SentimentResultId.generate(),item.newsId(),item.contentHash(),LanguageCode.ENGLISH,release,
                SentimentLabel.NEUTRAL,new BigDecimal("0.5"),BigDecimal.ZERO,now);
        assertThrows(RuntimeException.class,()->persistence.work().complete(item.newsId(),original.lease().orElseThrow().token(),result));
    }
}

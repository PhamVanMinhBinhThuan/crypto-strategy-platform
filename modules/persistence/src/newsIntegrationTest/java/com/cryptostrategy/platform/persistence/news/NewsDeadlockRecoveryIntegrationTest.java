package com.cryptostrategy.platform.persistence.news;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.news.api.error.NewsErrorCode;
import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.persistence.internal.news.NewsPersistenceExceptionTranslator;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class NewsDeadlockRecoveryIntegrationTest extends PostgresNewsTestSupport {
    @Test void claims_have_deterministic_eligibility_then_news_id_order_and_a_bounded_batch(){
        var release=release();var first=item(uniqueUrl(),"one",release,List.of());var second=item(uniqueUrl(),"two",release,List.of());var third=item(uniqueUrl(),"three",release,List.of());
        persistence.items().saveIfAbsent(first);persistence.items().saveIfAbsent(second);persistence.items().saveIfAbsent(third);
        var claimed=persistence.work().claim("worker",Instant.parse("2026-09-01T00:00:01Z"),Duration.ofSeconds(120),2);
        assertEquals(2,claimed.size());
        var expected=java.util.stream.Stream.of(first.newsId(),second.newsId(),third.newsId()).sorted(java.util.Comparator.comparing(NewsId::value)).limit(2).toList();
        assertEquals(expected,claimed.stream().map(NewsItem::newsId).toList());
    }

    @Test void postgres_deadlock_and_serialization_states_are_translated_to_recoverable_outcomes(){
        var translator=new NewsPersistenceExceptionTranslator();
        for(String state:List.of("40P01","40001")){
            var translated=translator.translate(new DataAccessResourceFailureException("controlled",new SQLException("controlled",state)));
            var recoverable=assertInstanceOf(NewsPersistenceExceptionTranslator.RecoverableNewsPersistenceException.class,translated);
            assertEquals(state,recoverable.sqlState());
        }
    }

    @Test void concurrent_release_activation_and_completion_follow_release_then_news_lock_order() throws Exception {
        var oldRelease=release();var item=item(uniqueUrl(),"lock-order",oldRelease,List.of());persistence.items().saveIfAbsent(item);
        var now=Instant.parse("2026-09-01T00:00:01Z");var claim=persistence.work().claim("worker",now,Duration.ofSeconds(120),1).getFirst();var token=claim.lease().orElseThrow().token();persistence.work().reserveAttempt(item.newsId(),token,item.contentHash(),oldRelease.modelVersion());
        var next=release();var result=new SentimentResult(SentimentResultId.generate(),item.newsId(),item.contentHash(),LanguageCode.ENGLISH,oldRelease,SentimentLabel.NEUTRAL,new java.math.BigDecimal("0.5"),java.math.BigDecimal.ZERO,now);
        var ready=new CountDownLatch(2);var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{
            var activation=pool.submit(()->{ready.countDown();start.await();persistence.releases().activateForEnglish(next.modelVersion());return null;});
            var completion=pool.submit(()->{ready.countDown();start.await();try{persistence.work().complete(item.newsId(),token,result);}catch(RuntimeException stale){assertFalse(stale instanceof NewsPersistenceExceptionTranslator.RecoverableNewsPersistenceException);}return null;});
            ready.await();start.countDown();activation.get(10,TimeUnit.SECONDS);completion.get(10,TimeUnit.SECONDS);
        }finally{pool.shutdownNow();}
        assertEquals(next.modelVersion(),jdbc.queryForObject("select target_model_version from news.news_item where news_item_id=?",String.class,item.newsId().value()));
    }
}

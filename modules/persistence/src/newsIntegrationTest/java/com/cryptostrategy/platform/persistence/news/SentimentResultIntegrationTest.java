package com.cryptostrategy.platform.persistence.news;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.news.api.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SentimentResultIntegrationTest extends PostgresNewsTestSupport {
    @Test void reservation_is_consumed_before_transport_and_deferral_does_not_consume_another_attempt(){
        var release=release();var item=item(uniqueUrl(),"attempt",release,List.of());persistence.items().saveIfAbsent(item);
        var now=Instant.parse("2026-09-01T00:00:01Z");var claimed=persistence.work().claim("worker",now,Duration.ofSeconds(120),1).getFirst();String token=claimed.lease().orElseThrow().token();
        assertTrue(persistence.work().reserveAttempt(item.newsId(),token,item.contentHash(),release.modelVersion()));
        assertEquals(1,jdbc.queryForObject("select attempt_count from news.news_item where news_item_id=?",Integer.class,item.newsId().value()));
        persistence.work().defer(item.newsId(),token,now.plusSeconds(5));
        assertTrue(persistence.work().claim("early",now.plusSeconds(4),Duration.ofSeconds(120),1).isEmpty());
        var retried=persistence.work().claim("retry",now.plusSeconds(5),Duration.ofSeconds(120),1).getFirst();
        assertEquals(1,retried.attemptCount());
    }

    @Test void equivalent_completion_is_idempotent_and_conflict_rolls_back_atomically(){
        var release=release();var item=item(uniqueUrl(),"result",release,List.of());persistence.items().saveIfAbsent(item);
        var now=Instant.parse("2026-09-01T00:00:01Z");var claim=persistence.work().claim("worker",now,Duration.ofSeconds(120),1).getFirst();var token=claim.lease().orElseThrow().token();
        assertTrue(persistence.work().reserveAttempt(item.newsId(),token,item.contentHash(),release.modelVersion()));
        var result=new SentimentResult(SentimentResultId.generate(),item.newsId(),item.contentHash(),LanguageCode.ENGLISH,release,SentimentLabel.POSITIVE,new BigDecimal("0.8"),new BigDecimal("0.6"),now);
        persistence.work().complete(item.newsId(),token,result);
        assertEquals("ANALYZED",jdbc.queryForObject("select analysis_status from news.news_item where news_item_id=?",String.class,item.newsId().value()));
        assertEquals(1,jdbc.queryForObject("select count(*) from news.sentiment_result where news_item_id=?",Integer.class,item.newsId().value()));
    }

    @Test void second_retry_uses_thirty_seconds_third_attempt_is_terminal_and_new_target_resets_budget(){
        var firstRelease=release();var item=item(uniqueUrl(),"budget",firstRelease,List.of());persistence.items().saveIfAbsent(item);
        var now=Instant.parse("2026-09-01T00:00:01Z");
        jdbc.update("update news.news_item set attempt_count=1 where news_item_id=?",item.newsId().value());
        var second=persistence.work().claim("second",now,Duration.ofSeconds(120),1).getFirst();var token=second.lease().orElseThrow().token();
        assertTrue(persistence.work().reserveAttempt(item.newsId(),token,item.contentHash(),firstRelease.modelVersion()));
        persistence.work().fail(item.newsId(),token,true,now.plusSeconds(30));
        assertTrue(persistence.work().claim("early",now.plusSeconds(29),Duration.ofSeconds(120),1).isEmpty());
        var third=persistence.work().claim("third",now.plusSeconds(30),Duration.ofSeconds(120),1).getFirst();var thirdToken=third.lease().orElseThrow().token();
        assertTrue(persistence.work().reserveAttempt(item.newsId(),thirdToken,item.contentHash(),firstRelease.modelVersion()));
        persistence.work().fail(item.newsId(),thirdToken,false,now.plusSeconds(30));
        assertEquals("FAILED",jdbc.queryForObject("select analysis_status from news.news_item where news_item_id=?",String.class,item.newsId().value()));

        var next=release();persistence.releases().activateForEnglish(next.modelVersion());
        var reset=jdbc.queryForMap("select target_model_version,analysis_status,attempt_count from news.news_item where news_item_id=?",item.newsId().value());
        assertEquals(next.modelVersion(),reset.get("target_model_version"));assertEquals("PENDING",reset.get("analysis_status"));assertEquals(0,reset.get("attempt_count"));
    }

    @Test void conflicting_preexisting_result_rolls_back_news_completion_but_equivalent_result_converges(){
        var release=release();var item=item(uniqueUrl(),"atomic",release,List.of());persistence.items().saveIfAbsent(item);
        var now=Instant.parse("2026-09-01T00:00:01Z");var claim=persistence.work().claim("worker",now,Duration.ofSeconds(120),1).getFirst();var token=claim.lease().orElseThrow().token();
        persistence.work().reserveAttempt(item.newsId(),token,item.contentHash(),release.modelVersion());
        jdbc.update("insert into news.sentiment_result(sentiment_result_id,news_item_id,content_hash,model_version,label,confidence,polarity_score,analyzed_at,language) values (?,?,?,?,?,?,?,?,?)",
                SentimentResultId.generate().value(),item.newsId().value(),item.contentHash().value(),release.modelVersion(),"NEGATIVE",new BigDecimal("0.8"),new BigDecimal("-0.6"),java.sql.Timestamp.from(now),"en");
        var conflicting=new SentimentResult(SentimentResultId.generate(),item.newsId(),item.contentHash(),LanguageCode.ENGLISH,release,SentimentLabel.POSITIVE,new BigDecimal("0.8"),new BigDecimal("0.6"),now);
        assertThrows(RuntimeException.class,()->persistence.work().complete(item.newsId(),token,conflicting));
        assertEquals("ANALYZING",jdbc.queryForObject("select analysis_status from news.news_item where news_item_id=?",String.class,item.newsId().value()));
        var equivalent=new SentimentResult(SentimentResultId.generate(),item.newsId(),item.contentHash(),LanguageCode.ENGLISH,release,SentimentLabel.NEGATIVE,new BigDecimal("0.8"),new BigDecimal("-0.6"),now);
        persistence.work().complete(item.newsId(),token,equivalent);
        assertEquals("ANALYZED",jdbc.queryForObject("select analysis_status from news.news_item where news_item_id=?",String.class,item.newsId().value()));
    }
}

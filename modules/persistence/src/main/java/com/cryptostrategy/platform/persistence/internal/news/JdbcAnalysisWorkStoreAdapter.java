package com.cryptostrategy.platform.persistence.internal.news;

import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.error.*;
import com.cryptostrategy.platform.news.api.port.out.AnalysisWorkStore;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public final class JdbcAnalysisWorkStoreAdapter implements AnalysisWorkStore {
    private final JdbcTemplate jdbc; private final TransactionTemplate transactions; private final NewsPersistenceExceptionTranslator errors;
    public JdbcAnalysisWorkStoreAdapter(JdbcTemplate jdbc, TransactionTemplate transactions, NewsPersistenceExceptionTranslator errors) { this.jdbc=jdbc; this.transactions=transactions; this.errors=errors; }
    @Override public List<NewsItem> claim(String owner, Instant now, Duration leaseDuration, int limit) {
        if (limit < 1 || limit > 25) throw new IllegalArgumentException("claim batch must be 1..25");
        try { return transactions.execute(status -> {
            jdbc.execute("set local lock_timeout='2s'"); jdbc.execute("set local statement_timeout='5s'");
            jdbc.update("""
                update news.news_item set analysis_status='FAILED',lease_owner=null,lease_token=null,
                       lease_expires_at=null,next_eligible_attempt=null
                 where analysis_status='ANALYZING' and lease_expires_at<=? and attempt_count>=3
                """,Timestamp.from(now));
            var ids = jdbc.queryForList("""
                  select news_item_id from news.news_item
                   where language='en' and target_model_version is not null and attempt_count<3 and (
                     analysis_status='PENDING'
                     or (analysis_status='FAILED_RETRYABLE' and next_eligible_attempt<=?)
                     or (analysis_status='ANALYZING' and lease_expires_at<=?))
                   order by case when analysis_status='ANALYZING' then lease_expires_at else coalesce(next_eligible_attempt,crawled_at) end, news_item_id
                   for update skip locked limit ?
                """, String.class, Timestamp.from(now), Timestamp.from(now), limit);
            var claimed = new java.util.ArrayList<NewsItem>();
            for (String id : ids) {
                String token = com.cryptostrategy.platform.domain.api.identity.Ulids.generate();
                claimed.add(jdbc.queryForObject("""
                    update news.news_item set analysis_status='ANALYZING',lease_owner=?,lease_token=?,lease_expires_at=?,next_eligible_attempt=null
                     where news_item_id=? returning *
                    """, (rs,row)->map(rs), owner, token, Timestamp.from(now.plus(leaseDuration)), id));
            }
            return List.copyOf(claimed);
        }); } catch (DataAccessException error) { throw errors.translate(error); }
    }
    private NewsItem map(java.sql.ResultSet rs) throws java.sql.SQLException {
        int attempts=rs.getInt("attempt_count");
        var lease=new AnalysisLease(rs.getString("lease_owner"),rs.getString("lease_token"),rs.getTimestamp("lease_expires_at").toInstant(),attempts,rs.getString("target_model_version"));
        return new NewsItem(new NewsId(rs.getString("news_item_id")),rs.getString("title"),rs.getString("content"),rs.getTimestamp("published_at").toInstant(),rs.getTimestamp("crawled_at").toInstant(),new ContentHash(rs.getString("content_hash")),AnalysisStatus.ANALYZING,new NewsSource(rs.getString("source")),new CanonicalNewsUrl(rs.getString("url")),new LanguageCode(rs.getString("language")),Optional.ofNullable(rs.getString("source_item_id")),Optional.ofNullable(rs.getString("target_model_version")),Optional.of(lease),Optional.empty(),attempts,List.of());
    }
    @Override public boolean reserveAttempt(NewsId id,String token,ContentHash hash,String model) {
        try{return jdbc.update("update news.news_item set attempt_count=attempt_count+1 where news_item_id=? and analysis_status='ANALYZING' and lease_token=? and content_hash=? and target_model_version=? and attempt_count<3",id.value(),token,hash.value(),model)==1;}
        catch(DataAccessException error){throw errors.translate(error);}
    }
    @Override public void defer(NewsId id,String token,Instant next) { transitionRetry(id,token,next); }
    @Override public void fail(NewsId id,String token,boolean retryable,Instant next) {
        try{if (retryable) transitionRetry(id,token,next); else fencedUpdate(jdbc.update("update news.news_item set analysis_status='FAILED',lease_owner=null,lease_token=null,lease_expires_at=null,next_eligible_attempt=null where news_item_id=? and lease_token=?",id.value(),token),id);}
        catch(DataAccessException error){throw errors.translate(error);}
    }
    private void transitionRetry(NewsId id,String token,Instant next) { try{fencedUpdate(jdbc.update("update news.news_item set analysis_status='FAILED_RETRYABLE',lease_owner=null,lease_token=null,lease_expires_at=null,next_eligible_attempt=? where news_item_id=? and lease_token=?",Timestamp.from(next),id.value(),token),id);}catch(DataAccessException error){throw errors.translate(error);} }
    @Override public void complete(NewsId id,String token,SentimentResult result) {
        try { transactions.executeWithoutResult(status -> {
            jdbc.execute("set local lock_timeout='2s'"); jdbc.execute("set local statement_timeout='5s'");
            var release = jdbc.queryForMap("select model_name,preprocessing_version,contract_version from news.sentiment_model_release where model_version=? for key share",result.release().modelVersion());
            if(!result.release().modelName().equals(release.get("model_name"))||!result.release().preprocessingVersion().equals(release.get("preprocessing_version"))||!result.release().contractVersion().equals(release.get("contract_version")))
                throw new NewsException(NewsErrorCode.INTEGRITY_CONFLICT,"Sentiment model release provenance conflict");
            Integer fenced = jdbc.query("select 1 from news.news_item where news_item_id=? and lease_token=? and content_hash=? and target_model_version=? for update",rs->rs.next()?1:null,id.value(),token,result.contentHash().value(),result.release().modelVersion());
            if (fenced == null) throw new IllegalStateException("Stale analysis completion");
            int inserted=jdbc.update("""
                insert into news.sentiment_result(sentiment_result_id,news_item_id,content_hash,model_version,label,confidence,polarity_score,analyzed_at,language)
                values (?,?,?,?,?,?,?,?,?) on conflict(news_item_id,content_hash,model_version) do nothing
                """,result.resultId().value(),id.value(),result.contentHash().value(),result.release().modelVersion(),result.label().name(),result.confidence(),result.polarityScore(),Timestamp.from(result.analyzedAt()),result.language().value());
            if(inserted==0) {
                Integer equivalent=jdbc.queryForObject("select count(*) from news.sentiment_result where news_item_id=? and content_hash=? and model_version=? and label=? and confidence=? and polarity_score=? and language=?",Integer.class,id.value(),result.contentHash().value(),result.release().modelVersion(),result.label().name(),result.confidence(),result.polarityScore(),result.language().value());
                if(equivalent==null||equivalent!=1) throw new IllegalStateException("Conflicting idempotent sentiment result");
            }
            if(jdbc.update("update news.news_item set analysis_status='ANALYZED',lease_owner=null,lease_token=null,lease_expires_at=null,next_eligible_attempt=null where news_item_id=? and lease_token=?",id.value(),token)!=1) throw new IllegalStateException("Stale completion");
        }); } catch(DataAccessException error){throw errors.translate(error);}
    }
    private static void fencedUpdate(int updated,NewsId id){if(updated!=1)throw new NewsException(NewsErrorCode.STALE_LEASE,"Stale News analysis lease",java.util.Map.of("newsId",id.value()),null);}
}

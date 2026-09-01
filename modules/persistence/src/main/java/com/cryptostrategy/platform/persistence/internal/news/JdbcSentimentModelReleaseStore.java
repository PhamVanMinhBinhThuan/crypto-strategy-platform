package com.cryptostrategy.platform.persistence.internal.news;

import com.cryptostrategy.platform.news.api.model.SentimentModelRelease;
import com.cryptostrategy.platform.news.api.port.out.SentimentModelReleaseStore;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public final class JdbcSentimentModelReleaseStore implements SentimentModelReleaseStore {
    private final JdbcTemplate jdbc; private final TransactionTemplate transactions; private final NewsPersistenceExceptionTranslator errors;
    public JdbcSentimentModelReleaseStore(JdbcTemplate jdbc, TransactionTemplate transactions, NewsPersistenceExceptionTranslator errors) {
        this.jdbc=jdbc; this.transactions=transactions; this.errors=errors;
    }
    @Override public void registerOrVerify(SentimentModelRelease release) {
        try { transactions.executeWithoutResult(status -> {
            jdbc.update("insert into news.sentiment_model_release(model_version,model_name,preprocessing_version,contract_version) values (?,?,?,?) on conflict do nothing",
                release.modelVersion(), release.modelName(), release.preprocessingVersion(), release.contractVersion());
            Map<String,Object> row = jdbc.queryForMap("select model_name,preprocessing_version,contract_version from news.sentiment_model_release where model_version=? for key share", release.modelVersion());
            if (!release.modelName().equals(row.get("model_name")) || !release.preprocessingVersion().equals(row.get("preprocessing_version")) || !release.contractVersion().equals(row.get("contract_version")))
                throw new IllegalStateException("Model version is already registered with different immutable provenance");
        }); } catch (DataAccessException error) { throw errors.translate(error); }
    }
    @Override public void activateForEnglish(String modelVersion) {
        try{transactions.executeWithoutResult(status->{
            jdbc.execute("set local lock_timeout='2s'");jdbc.execute("set local statement_timeout='5s'");
            Integer release=jdbc.queryForObject("select 1 from news.sentiment_model_release where model_version=? for key share",Integer.class,modelVersion);
            if(release==null)throw new IllegalStateException("Missing sentiment model release");
            jdbc.update("""
                with targets as (
                    select news_item_id from news.news_item
                     where language='en' and target_model_version is distinct from ?
                     order by news_item_id for update
                )
                update news.news_item n
                   set target_model_version=?,analysis_status='PENDING',attempt_count=0,
                       lease_owner=null,lease_token=null,lease_expires_at=null,next_eligible_attempt=null
                  from targets t where n.news_item_id=t.news_item_id
                """,modelVersion,modelVersion);
        });}catch(DataAccessException error){throw errors.translate(error);}
    }
}

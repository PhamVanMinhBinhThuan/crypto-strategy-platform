package com.cryptostrategy.platform.persistence.internal.news;

import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.out.SentimentAuditStore;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcSentimentAuditStore implements SentimentAuditStore {
    private final JdbcTemplate jdbc; public JdbcSentimentAuditStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @Override public Optional<SentimentAuditRecord> findLatest(NewsId id){return jdbc.query("""
        select r.sentiment_result_id,r.news_item_id,r.language,r.content_hash,r.label,r.confidence,r.polarity_score,r.analyzed_at,
               m.model_version,m.model_name,m.preprocessing_version,m.contract_version
          from news.sentiment_result r join news.sentiment_model_release m on m.model_version=r.model_version
         where r.news_item_id=? order by r.analyzed_at desc limit 1
        """,(rs,row)->new SentimentAuditRecord(new SentimentResultId(rs.getString(1)),new NewsId(rs.getString(2)),new LanguageCode(rs.getString(3)),new ContentHash(rs.getString(4)),new SentimentModelRelease(rs.getString(9),rs.getString(10),rs.getString(11),rs.getString(12)),SentimentLabel.valueOf(rs.getString(5)),rs.getBigDecimal(6),rs.getBigDecimal(7),rs.getTimestamp(8).toInstant()),id.value()).stream().findFirst();}
}

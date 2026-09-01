package com.cryptostrategy.platform.news.api.port.in;

import com.cryptostrategy.platform.news.api.model.NewsId;
import com.cryptostrategy.platform.news.api.model.SentimentAuditRecord;
import java.util.Optional;

/** Protected application boundary for immutable Sentiment provenance. */
public interface GetSentimentAuditUseCase {
    Optional<SentimentAuditRecord> findLatest(NewsId newsId);
}

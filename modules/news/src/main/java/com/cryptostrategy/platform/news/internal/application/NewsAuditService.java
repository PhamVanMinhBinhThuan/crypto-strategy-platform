package com.cryptostrategy.platform.news.internal.application;

import com.cryptostrategy.platform.news.api.model.NewsId;
import com.cryptostrategy.platform.news.api.model.SentimentAuditRecord;
import com.cryptostrategy.platform.news.api.port.in.GetSentimentAuditUseCase;
import com.cryptostrategy.platform.news.api.port.out.SentimentAuditStore;
import java.util.Objects;
import java.util.Optional;

public final class NewsAuditService implements GetSentimentAuditUseCase {
    private final SentimentAuditStore store;
    public NewsAuditService(SentimentAuditStore store){this.store=Objects.requireNonNull(store,"store");}
    @Override public Optional<SentimentAuditRecord> findLatest(NewsId newsId){return store.findLatest(Objects.requireNonNull(newsId,"newsId"));}
}

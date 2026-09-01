package com.cryptostrategy.platform.news.api.port.out;

import com.cryptostrategy.platform.news.api.model.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface AnalysisWorkStore {
    List<NewsItem> claim(String owner, Instant now, Duration leaseDuration, int limit);
    boolean reserveAttempt(NewsId newsId, String leaseToken, ContentHash hash, String modelVersion);
    void defer(NewsId newsId, String leaseToken, Instant nextEligibleAt);
    void fail(NewsId newsId, String leaseToken, boolean retryable, Instant nextEligibleAt);
    void complete(NewsId newsId, String leaseToken, SentimentResult result);
}

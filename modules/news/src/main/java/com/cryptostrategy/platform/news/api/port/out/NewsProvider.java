package com.cryptostrategy.platform.news.api.port.out;

import com.cryptostrategy.platform.news.api.model.NewsSource;
import com.cryptostrategy.platform.news.api.model.ProviderNewsItem;
import java.time.Instant;
import java.util.List;

public interface NewsProvider {
    NewsSource source();
    List<ProviderNewsItem> fetchSince(Instant since);
}

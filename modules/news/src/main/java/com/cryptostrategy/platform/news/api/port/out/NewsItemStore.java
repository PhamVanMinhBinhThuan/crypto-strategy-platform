package com.cryptostrategy.platform.news.api.port.out;

import com.cryptostrategy.platform.news.api.model.NewsItem;

public interface NewsItemStore {
    SaveOutcome saveIfAbsent(NewsItem item);
    enum SaveOutcome { INSERTED, ALREADY_PRESENT, CONFLICT }
}

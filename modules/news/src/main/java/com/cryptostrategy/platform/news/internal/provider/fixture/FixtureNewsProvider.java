package com.cryptostrategy.platform.news.internal.provider.fixture;

import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.out.NewsProvider;
import java.time.Instant;
import java.util.List;

public final class FixtureNewsProvider implements NewsProvider {
    private final NewsSource source; private final List<ProviderNewsItem> items;
    public FixtureNewsProvider(NewsSource source,List<ProviderNewsItem> items){this.source=source;this.items=List.copyOf(items);}
    @Override public NewsSource source(){return source;}
    @Override public List<ProviderNewsItem> fetchSince(Instant since){return items.stream().filter(item->!item.publishedAt().isBefore(since)).toList();}
}

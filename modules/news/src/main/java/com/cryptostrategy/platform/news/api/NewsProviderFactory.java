package com.cryptostrategy.platform.news.api;

import com.cryptostrategy.platform.news.api.model.NewsSource;
import com.cryptostrategy.platform.news.api.port.out.NewsProvider;
import com.cryptostrategy.platform.news.internal.provider.rss.RssNewsProvider;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/** Public construction boundary for provider adapters owned by the News module. */
public final class NewsProviderFactory {
    private NewsProviderFactory() {}
    public static NewsProvider rss(String source,URI feed,String language,List<String> assetSymbols,HttpClient http,Duration timeout){
        return new RssNewsProvider(new NewsSource(source),feed,language,assetSymbols,http,timeout);
    }
}

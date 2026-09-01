package com.cryptostrategy.platform.persistence.internal.news;

final class NewsSql {
    private NewsSql() {}

    static final String INSERT_ITEM = """
            insert into news.news_item(
                news_item_id,source,source_item_id,url,title,content,content_hash,
                published_at,crawled_at,analysis_status,language,target_model_version)
            values (?,?,?,?,?,?,?,?,?,'PENDING',?,?)
            on conflict (url) do nothing
            """;
    static final String FIND_EXISTING_BY_URL = """
            select news_item_id, content_hash, source, source_item_id
            from news.news_item where url=?
            """;
    static final String INSERT_ASSET = """
            insert into news.news_item_asset(news_item_id,asset_id,relevance_score)
            values (?,?,?) on conflict (news_item_id,asset_id) do nothing
            """;
}

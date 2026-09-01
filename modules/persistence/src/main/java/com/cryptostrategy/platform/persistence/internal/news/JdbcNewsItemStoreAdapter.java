package com.cryptostrategy.platform.persistence.internal.news;

import com.cryptostrategy.platform.news.api.model.NewsItem;
import com.cryptostrategy.platform.news.api.port.out.NewsItemStore;
import java.sql.Timestamp;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public final class JdbcNewsItemStoreAdapter implements NewsItemStore {
    private final JdbcTemplate jdbc; private final TransactionTemplate transactions; private final NewsPersistenceExceptionTranslator errors;
    public JdbcNewsItemStoreAdapter(JdbcTemplate jdbc, TransactionTemplate transactions, NewsPersistenceExceptionTranslator errors) {
        this.jdbc = jdbc; this.transactions = transactions; this.errors = errors;
    }
    @Override public SaveOutcome saveIfAbsent(NewsItem item) {
        try {
            return transactions.execute(status -> {
                int inserted = jdbc.update(NewsSql.INSERT_ITEM,
                    item.newsId().value(), item.source().value(), item.sourceItemId().orElse(null), item.url().toString(), item.title(), item.content(),
                    item.contentHash().value(), Timestamp.from(item.publishedAt()), Timestamp.from(item.crawledAt()), item.language().value(), item.targetModelVersion().orElse(null));
                if (inserted == 0) {
                    var existing = jdbc.queryForObject(NewsSql.FIND_EXISTING_BY_URL, NewsRows.EXISTING_NEWS, item.url().toString());
                    return existing != null && item.contentHash().value().equals(existing.contentHash())
                            ? SaveOutcome.ALREADY_PRESENT : SaveOutcome.CONFLICT;
                }
                item.relatedAssets().stream().sorted(java.util.Comparator.comparing(a -> a.assetId().value())).forEach(asset ->
                    jdbc.update(NewsSql.INSERT_ASSET,
                        item.newsId().value(), asset.assetId().value(), asset.relevanceScore().orElse(null)));
                return SaveOutcome.INSERTED;
            });
        } catch (DataAccessException error) { throw errors.translate(error); }
    }
}

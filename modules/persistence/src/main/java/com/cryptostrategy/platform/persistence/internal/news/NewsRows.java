package com.cryptostrategy.platform.persistence.internal.news;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

final class NewsRows {
    private NewsRows() {}

    static final RowMapper<ExistingNews> EXISTING_NEWS = (result, row) -> existingNews(result);

    private static ExistingNews existingNews(ResultSet result) throws SQLException {
        return new ExistingNews(result.getString("news_item_id"), result.getString("content_hash"),
                result.getString("source"), result.getString("source_item_id"));
    }

    record ExistingNews(String newsItemId, String contentHash, String source, String sourceItemId) {}
}

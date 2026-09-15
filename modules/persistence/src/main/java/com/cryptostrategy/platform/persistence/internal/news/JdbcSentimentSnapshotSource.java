package com.cryptostrategy.platform.persistence.internal.news;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.news.api.model.ContentHash;
import com.cryptostrategy.platform.news.api.model.NewsId;
import com.cryptostrategy.platform.news.api.model.SentimentModelRelease;
import com.cryptostrategy.platform.news.api.model.SentimentResultId;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotObservation;
import com.cryptostrategy.platform.news.api.port.out.SentimentSnapshotSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcSentimentSnapshotSource implements SentimentSnapshotSource {
    private final JdbcTemplate jdbc;

    public JdbcSentimentSnapshotSource(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Override
    public List<SentimentSnapshotObservation> findForSnapshot(AssetId assetId,
            Instant publicationCutoff, SentimentModelRelease release) {
        return jdbc.query("""
                select selected.news_item_id, selected.sentiment_result_id, selected.asset_id,
                       selected.published_at, selected.analyzed_at, selected.content_hash,
                       selected.polarity_score, selected.confidence
                  from (
                    select distinct on (n.news_item_id)
                           n.news_item_id, r.sentiment_result_id, a.asset_id, n.published_at,
                           r.analyzed_at, r.content_hash, r.polarity_score, r.confidence
                      from news.news_item n
                      join news.news_item_asset a on a.news_item_id = n.news_item_id
                      join news.sentiment_result r on r.news_item_id = n.news_item_id
                     where a.asset_id = ?
                       and n.analysis_status = 'ANALYZED'
                       and n.published_at <= ?
                       and r.model_version = ?
                     order by n.news_item_id, r.analyzed_at desc, r.sentiment_result_id desc
                  ) selected
                 order by selected.published_at, selected.news_item_id
                """, (result, row) -> new SentimentSnapshotObservation(
                        new NewsId(result.getString("news_item_id")),
                        new SentimentResultId(result.getString("sentiment_result_id")),
                        new AssetId(result.getString("asset_id")),
                        result.getTimestamp("published_at").toInstant(),
                        result.getTimestamp("analyzed_at").toInstant(),
                        new ContentHash(result.getString("content_hash")),
                        result.getBigDecimal("polarity_score"),
                        result.getBigDecimal("confidence")),
                assetId.value(), Timestamp.from(publicationCutoff), release.modelVersion());
    }
}

package com.cryptostrategy.platform.persistence.internal.news;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.news.api.model.ContentHash;
import com.cryptostrategy.platform.news.api.model.NewsId;
import com.cryptostrategy.platform.news.api.model.SentimentModelRelease;
import com.cryptostrategy.platform.news.api.model.SentimentResultId;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshot;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotId;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotObservation;
import com.cryptostrategy.platform.news.api.port.out.SentimentSnapshotStore;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public final class JdbcSentimentSnapshotStore implements SentimentSnapshotStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final NewsPersistenceExceptionTranslator errors;

    public JdbcSentimentSnapshotStore(JdbcTemplate jdbc, TransactionTemplate transactions,
            NewsPersistenceExceptionTranslator errors) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.transactions = Objects.requireNonNull(transactions);
        this.errors = Objects.requireNonNull(errors);
    }

    @Override
    public SentimentSnapshot save(SentimentSnapshot snapshot) {
        Objects.requireNonNull(snapshot);
        try {
            return transactions.execute(status -> {
                int inserted = jdbc.update("""
                        insert into news.sentiment_snapshot(
                            sentiment_snapshot_id, schema_version, asset_id, model_version,
                            publication_cutoff, observation_count, fingerprint, created_at
                        ) values (?,?,?,?,?,?,?,?)
                        on conflict (fingerprint) do nothing
                        """, snapshot.snapshotId().value(), snapshot.schemaVersion(),
                        snapshot.assetId().value(), snapshot.release().modelVersion(),
                        Timestamp.from(snapshot.publicationCutoff()), snapshot.observationCount(),
                        snapshot.fingerprint(), Timestamp.from(snapshot.createdAt()));
                if (inserted == 0) {
                    return findByFingerprint(snapshot.fingerprint()).orElseThrow(() ->
                            new IllegalStateException("Snapshot fingerprint conflict was not readable"));
                }
                jdbc.batchUpdate("""
                        insert into news.sentiment_snapshot_observation(
                            sentiment_snapshot_id, ordinal, news_item_id, sentiment_result_id,
                            published_at, analyzed_at, content_hash, polarity_score, confidence
                        ) values (?,?,?,?,?,?,?,?,?)
                        """, snapshot.observations(), snapshot.observations().size(),
                        (statement, observation) -> {
                            int ordinal = snapshot.observations().indexOf(observation) + 1;
                            statement.setString(1, snapshot.snapshotId().value());
                            statement.setInt(2, ordinal);
                            statement.setString(3, observation.newsId().value());
                            statement.setString(4, observation.sentimentResultId().value());
                            statement.setTimestamp(5, Timestamp.from(observation.publishedAt()));
                            statement.setTimestamp(6, Timestamp.from(observation.analyzedAt()));
                            statement.setString(7, observation.contentHash().value());
                            statement.setBigDecimal(8, observation.polarityScore());
                            statement.setBigDecimal(9, observation.confidence());
                        });
                return snapshot;
            });
        } catch (DataAccessException error) {
            throw errors.translate(error);
        }
    }

    @Override
    public Optional<SentimentSnapshot> find(SentimentSnapshotId snapshotId) {
        Objects.requireNonNull(snapshotId);
        return findBy("where s.sentiment_snapshot_id = ?", snapshotId.value());
    }

    private Optional<SentimentSnapshot> findByFingerprint(String fingerprint) {
        return findBy("where s.fingerprint = ?", fingerprint);
    }

    private Optional<SentimentSnapshot> findBy(String predicate, Object argument) {
        List<SnapshotRow> rows = jdbc.query("""
                select s.sentiment_snapshot_id, s.schema_version, s.asset_id,
                       s.publication_cutoff, s.fingerprint, s.created_at,
                       m.model_version, m.model_name, m.preprocessing_version, m.contract_version
                  from news.sentiment_snapshot s
                  join news.sentiment_model_release m on m.model_version = s.model_version
                """ + predicate, (result, row) -> new SnapshotRow(
                        new SentimentSnapshotId(result.getString("sentiment_snapshot_id")),
                        result.getString("schema_version"),
                        new AssetId(result.getString("asset_id")),
                        result.getTimestamp("publication_cutoff").toInstant(),
                        result.getString("fingerprint"),
                        result.getTimestamp("created_at").toInstant(),
                        new SentimentModelRelease(result.getString("model_version"),
                                result.getString("model_name"),
                                result.getString("preprocessing_version"),
                                result.getString("contract_version"))), argument);
        if (rows.isEmpty()) return Optional.empty();
        SnapshotRow row = rows.getFirst();
        List<SentimentSnapshotObservation> observations = jdbc.query("""
                select o.news_item_id, o.sentiment_result_id, s.asset_id, o.published_at,
                       o.analyzed_at, o.content_hash, o.polarity_score, o.confidence
                  from news.sentiment_snapshot_observation o
                  join news.sentiment_snapshot s
                    on s.sentiment_snapshot_id = o.sentiment_snapshot_id
                 where o.sentiment_snapshot_id = ?
                 order by o.ordinal
                """, (result, index) -> new SentimentSnapshotObservation(
                        new NewsId(result.getString("news_item_id")),
                        new SentimentResultId(result.getString("sentiment_result_id")),
                        new AssetId(result.getString("asset_id")),
                        result.getTimestamp("published_at").toInstant(),
                        result.getTimestamp("analyzed_at").toInstant(),
                        new ContentHash(result.getString("content_hash")),
                        result.getBigDecimal("polarity_score"),
                        result.getBigDecimal("confidence")), row.snapshotId().value());
        return Optional.of(new SentimentSnapshot(row.snapshotId(), row.schemaVersion(), row.assetId(),
                row.release(), row.publicationCutoff(), observations, row.fingerprint(), row.createdAt()));
    }

    private record SnapshotRow(SentimentSnapshotId snapshotId, String schemaVersion, AssetId assetId,
            java.time.Instant publicationCutoff, String fingerprint, java.time.Instant createdAt,
            SentimentModelRelease release) {}
}

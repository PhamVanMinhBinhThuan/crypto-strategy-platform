package com.cryptostrategy.platform.persistence.news;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.news.api.NewsModuleFactory;
import com.cryptostrategy.platform.news.api.model.LanguageCode;
import com.cryptostrategy.platform.news.api.model.RelatedNewsAsset;
import com.cryptostrategy.platform.news.api.model.SentimentLabel;
import com.cryptostrategy.platform.news.api.model.SentimentResult;
import com.cryptostrategy.platform.news.api.model.SentimentResultId;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshot;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotId;
import com.cryptostrategy.platform.news.api.model.SentimentSnapshotObservation;
import com.cryptostrategy.platform.news.api.port.in.CreateSentimentSnapshotUseCase;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

class SentimentSnapshotIntegrationTest extends PostgresNewsTestSupport {
    private static final Instant ANALYZED_AT = Instant.parse("2026-09-01T00:00:01Z");
    private static final Instant CUTOFF = Instant.parse("2026-09-01T00:00:00Z");

    @Test
    void snapshot_round_trips_idempotently_and_rejects_mutation() {
        var release = release();
        var assetId = new AssetId(insertAsset());
        var item = item(uniqueUrl(), "snapshot-round-trip", release,
                List.of(new RelatedNewsAsset(assetId, Optional.of(new BigDecimal("0.9")))));
        persistence.items().saveIfAbsent(item);
        completeAnalysis(item, release,
                new BigDecimal("0.8000000000"), new BigDecimal("0.6000000000"));

        var snapshots = NewsModuleFactory.snapshotUseCase(
                persistence.snapshotSource(), persistence.snapshots(),
                Clock.fixed(Instant.parse("2026-09-02T00:00:00Z"), ZoneOffset.UTC));
        var command = new CreateSentimentSnapshotUseCase.Command(assetId, CUTOFF, release);

        SentimentSnapshot created = snapshots.create(command);
        SentimentSnapshot repeated = snapshots.create(command);
        SentimentSnapshot loaded = snapshots.get(created.snapshotId());

        assertEquals(created.snapshotId(), repeated.snapshotId());
        assertEquals(created, loaded);
        assertEquals(1, loaded.observationCount());
        assertEquals(item.newsId(), loaded.observations().getFirst().newsId());
        assertEquals("0.6", loaded.observations().getFirst().polarityScore().toPlainString());

        assertThrows(DataAccessException.class, () -> jdbc.update(
                "update news.sentiment_snapshot set publication_cutoff = publication_cutoff + interval '1 second' where sentiment_snapshot_id = ?",
                created.snapshotId().value()));
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "delete from news.sentiment_snapshot_observation where sentiment_snapshot_id = ?",
                created.snapshotId().value()));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from news.sentiment_snapshot_observation where sentiment_snapshot_id = ?",
                Integer.class, created.snapshotId().value()));
    }

    @Test
    void unrelated_asset_evidence_rolls_back_the_entire_snapshot() {
        var release = release();
        var evidenceAsset = new AssetId(insertAsset());
        var unrelatedAsset = new AssetId(insertAsset());
        var item = item(uniqueUrl(), "snapshot-ownership", release,
                List.of(new RelatedNewsAsset(evidenceAsset, Optional.empty())));
        persistence.items().saveIfAbsent(item);
        SentimentResult result = completeAnalysis(item, release,
                new BigDecimal("0.7000000000"), new BigDecimal("-0.4000000000"));

        var observation = new SentimentSnapshotObservation(item.newsId(), result.resultId(),
                unrelatedAsset, item.publishedAt(), result.analyzedAt(), item.contentHash(),
                result.polarityScore(), result.confidence());
        var snapshotId = SentimentSnapshotId.generate();
        var invalid = new SentimentSnapshot(snapshotId, SentimentSnapshot.SCHEMA_VERSION,
                unrelatedAsset, release, CUTOFF, List.of(observation),
                "sha256:" + "a".repeat(64), Instant.parse("2026-09-02T00:00:00Z"));

        assertThrows(RuntimeException.class, () -> persistence.snapshots().save(invalid));
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from news.sentiment_snapshot where sentiment_snapshot_id = ?",
                Integer.class, snapshotId.value()));
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from news.sentiment_snapshot_observation where sentiment_snapshot_id = ?",
                Integer.class, snapshotId.value()));
    }

    private SentimentResult completeAnalysis(
            com.cryptostrategy.platform.news.api.model.NewsItem item,
            com.cryptostrategy.platform.news.api.model.SentimentModelRelease release,
            BigDecimal confidence,
            BigDecimal polarity) {
        var claim = persistence.work().claim("snapshot-worker", ANALYZED_AT,
                Duration.ofSeconds(120), 1).getFirst();
        String token = claim.lease().orElseThrow().token();
        persistence.work().reserveAttempt(item.newsId(), token, item.contentHash(),
                release.modelVersion());
        var result = new SentimentResult(SentimentResultId.generate(), item.newsId(),
                item.contentHash(), LanguageCode.ENGLISH, release,
                polarity.signum() >= 0 ? SentimentLabel.POSITIVE : SentimentLabel.NEGATIVE,
                confidence, polarity, ANALYZED_AT);
        persistence.work().complete(item.newsId(), token, result);
        return result;
    }
}

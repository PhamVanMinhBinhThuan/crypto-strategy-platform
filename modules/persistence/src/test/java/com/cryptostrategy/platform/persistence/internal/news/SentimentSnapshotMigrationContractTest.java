package com.cryptostrategy.platform.persistence.internal.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SentimentSnapshotMigrationContractTest {
    private final Path migration = Path.of(System.getProperty("user.dir"))
            .resolve("../..")
            .normalize()
            .resolve("supabase/migrations/20260909000100_f016_sentiment_snapshot.sql");

    @Test
    void definesImmutableModelPinnedSnapshotAndOrderedEvidence() throws Exception {
        String sql = Files.readString(migration).toLowerCase();

        assertThat(sql)
                .contains("create table news.sentiment_snapshot")
                .contains("create table news.sentiment_snapshot_observation")
                .contains("references news.sentiment_model_release(model_version)")
                .contains("unique (sentiment_snapshot_id, news_item_id)")
                .contains("snapshot observation is after publication cutoff")
                .contains("snapshot observation is unrelated to snapshot asset")
                .contains("sentiment_snapshot_immutable")
                .contains("sentiment_snapshot_observation_immutable")
                .contains("revoke all on news.sentiment_snapshot from anon, authenticated");
    }
}

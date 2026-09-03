package com.cryptostrategy.platform.persistence.internal.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SearchMigrationContractTest {
    private static final String F009 = "20260902000100_f009_standalone_backtest.sql";
    private static final String F010 = "20260903000100_f010_search_coordinator.sql";

    private final Path migrations = Path.of(System.getProperty("user.dir"))
            .resolve("../..")
            .normalize()
            .resolve("supabase/migrations");

    @Test
    void preservesEveryPreF010MigrationChecksum() throws Exception {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("20260827000100_create_database_baseline.sql", "a6ade694888cc6d306e3c6e3dce5131ae2ee43353494df3cb2b56de90d31771a");
        expected.put("20260828000100_add_user_strategies_and_jobs.sql", "b788494c7bcb3c7cfaae98463e3e8786819290cf65ab578d7e5656a23fc59df6");
        expected.put("20260830000100_add_news_sentiment_workflow.sql", "d7455a31be874408f8e77e2c70b6757a346fae147ffa867150123cc6f7969902");
        expected.put("20260830000100_f005_schema_alignment.sql", "db464e34d87973b460b9b156428a9e03bd00876abfd42b40e0681586bd94d87e");
        expected.put("20260830000200_f005_legacy_attempt_backfill.sql", "2ff051903ba4cd263b1aa1b0ade85e1225b169ec6002172922bcca21f87a7412");
        expected.put("20260831000100_f006_prerequisite_integrity.sql", "3d6a49cb608724a300ead5835ac520bad0d2027b154ba3a127b72126cc8539ba");
        expected.put("20260901000100_f006_backtest_evaluation_leaderboard.sql", "3dc87acad87ac32c941795fcc4da77cd142c61dc7d4752ee1036b61f2c557bce");
        expected.put("20260901000200_f006_review_remediation.sql", "c7f4d4332ef6e5fc09d8272e101245a2df6302df72cb047e83b8f078fde32478");
        expected.put("20260901000300_f006_reproduction_fks.sql", "90026a5b55dbd3ab3ba9d04c811cdc6cb360aab5dfff2f79e39914ac1375e89a");
        expected.put(F009, "d2c9d30fb1b98c111dc0d2eefc305ea2206918a7b7fcb4e9323cf27387897e40");

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            assertThat(sha256(migrations.resolve(entry.getKey())))
                    .as("checksum của migration đã áp dụng %s", entry.getKey())
                    .isEqualTo(entry.getValue());
        }
    }

    @Test
    void ordersF010AfterF009WithoutEditingPredecessors() throws Exception {
        try (var files = Files.list(migrations)) {
            var names = files.map(path -> path.getFileName().toString()).sorted().toList();
            assertThat(names).contains(F009, F010);
            assertThat(names.indexOf(F010)).isGreaterThan(names.indexOf(F009));
        }
    }

    @Test
    void f010DeclaresSearchOwnedStateFencesAndRecoveryIndexes() throws Exception {
        String sql = Files.readString(migrations.resolve(F010)).toLowerCase();

        assertThat(sql)
                .contains("create schema if not exists search")
                .contains("create table search.search_run")
                .contains("create table search.coordination_decision")
                .contains("create table search.reproduction_verification")
                .contains("version bigint not null")
                .contains("search_run_recovery_idx")
                .contains("reproduction_verification_recovery_idx")
                .contains("revoke all on all tables in schema search from anon, authenticated");
    }

    private static String sha256(Path path) throws Exception {
        byte[] content = Files.readAllBytes(path);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
        return HexFormat.of().formatHex(digest);
    }
}

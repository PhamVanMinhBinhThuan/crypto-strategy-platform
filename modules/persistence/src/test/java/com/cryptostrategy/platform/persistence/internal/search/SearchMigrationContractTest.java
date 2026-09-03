package com.cryptostrategy.platform.persistence.internal.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        expected.put("20260827000100_create_database_baseline.sql", "c10b47d4081f059939ee3e3cda5c24fddfc07f7a46de2d9a8e7dffe1c87c0d8a");
        expected.put("20260828000100_add_user_strategies_and_jobs.sql", "6bd5fb3595e8dcf5f7cc373854a3db22b37aa41014cbec71633f8e270164935a");
        expected.put("20260830000100_add_news_sentiment_workflow.sql", "646167339a12e14883d032e450c4ee3814297d301dff4b39907971f3e9b4fca6");
        expected.put("20260830000100_f005_schema_alignment.sql", "7c597abacb0b7779d92a10c958f5814cb91bb42726223b9ed4ab50b3fad49faf");
        expected.put("20260830000200_f005_legacy_attempt_backfill.sql", "7daec992357b76c99d00590f3a2aca5df48b11c3891b69594e03db2804c26309");
        expected.put("20260831000100_f006_prerequisite_integrity.sql", "28f26dd0987f3b2e25ab6bd500f4db4bc563feeb20fb600988c5ca54ac749869");
        expected.put("20260901000100_f006_backtest_evaluation_leaderboard.sql", "73684cf3cd20d45b03a0ae901194aae1c18e868664feba5dae4036d63ebed62d");
        expected.put("20260901000200_f006_review_remediation.sql", "1c2b4792ee1742939511c6625c1ec95d6005669a395934a3d086da756c0a1c99");
        expected.put("20260901000300_f006_reproduction_fks.sql", "f9d410c623b6ca122110cc9999568aa66ffc356108e7793b7b331196105833b2");
        expected.put(F009, "8a34be9da715ab872b8587cda00dd4986d46c442bdd864e07f6af4f5a76ff352");

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            assertThat(sha256(migrations.resolve(entry.getKey())))
                    .as("checksum của migration đã áp dụng %s", entry.getKey())
                    .isEqualTo(entry.getValue());
        }
    }

    @Test
    void ordersF010AfterF009WithoutEditingPredecessors() throws Exception {
        try (var files = Files.list(migrations)) {
            var names = files.map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();

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

    @Test
    void checksumIgnoresCheckoutLineEndingsButDetectsSqlChanges(@TempDir Path directory) throws Exception {
        String sql = "-- Migration\nSELECT 1;\n";
        Path lf = Files.writeString(directory.resolve("lf.sql"), sql);
        Path crlf = Files.writeString(directory.resolve("crlf.sql"), sql.replace("\n", "\r\n"));
        Path changed = Files.writeString(directory.resolve("changed.sql"), sql.replace("SELECT 1", "SELECT 2"));

        assertThat(sha256(crlf)).isEqualTo(sha256(lf));
        assertThat(sha256(changed)).isNotEqualTo(sha256(lf));
    }

    private static String sha256(Path path) throws Exception {
        // Match Git's LF content regardless of the checkout's core.autocrlf setting.
        byte[] content = Files.readString(path, StandardCharsets.UTF_8)
                .replace("\r\n", "\n")
                .getBytes(StandardCharsets.UTF_8);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
        return HexFormat.of().formatHex(digest);
    }
}

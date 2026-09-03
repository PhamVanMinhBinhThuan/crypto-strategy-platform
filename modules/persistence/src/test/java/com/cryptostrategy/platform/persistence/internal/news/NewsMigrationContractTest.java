package com.cryptostrategy.platform.persistence.internal.news;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class NewsMigrationContractTest {
    private final Path migrations=Path.of(System.getProperty("user.dir")).resolve("../..").normalize().resolve("supabase/migrations");
    @Test void preserves_applied_migrations_and_orders_f008_after_its_prerequisites() throws Exception {
        assertEquals("a6ade694888cc6d306e3c6e3dce5131ae2ee43353494df3cb2b56de90d31771a",hash(migrations.resolve("20260827000100_create_database_baseline.sql")));
        assertEquals("b788494c7bcb3c7cfaae98463e3e8786819290cf65ab578d7e5656a23fc59df6",hash(migrations.resolve("20260828000100_add_user_strategies_and_jobs.sql")));
        var names=Files.list(migrations).map(path->path.getFileName().toString()).sorted().toList();
        String f008="20260830000100_add_news_sentiment_workflow.sql";
        assertTrue(names.contains(f008));
        assertTrue(names.indexOf(f008)>names.indexOf("20260828000100_add_user_strategies_and_jobs.sql"));
    }
    @Test void forward_migration_contains_required_durable_and_immutable_contracts() throws Exception {
        String sql=Files.readString(migrations.resolve("20260830000100_add_news_sentiment_workflow.sql"));
        for(String required:new String[]{"sentiment_model_release","FAILED_RETRYABLE","lease_token","next_eligible_attempt","news_analysis_claim_idx","sentiment_result_immutable"})assertTrue(sql.contains(required),required);
    }
    private static String hash(Path path)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));}
}

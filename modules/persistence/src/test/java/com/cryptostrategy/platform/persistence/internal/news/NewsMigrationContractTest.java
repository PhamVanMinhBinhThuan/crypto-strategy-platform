package com.cryptostrategy.platform.persistence.internal.news;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class NewsMigrationContractTest {
    private final Path migrations=Path.of(System.getProperty("user.dir")).resolve("../..").normalize().resolve("supabase/migrations");
    @Test void preserves_applied_migrations_and_orders_f008_after_its_prerequisites() throws Exception {
        assertEquals("c10b47d4081f059939ee3e3cda5c24fddfc07f7a46de2d9a8e7dffe1c87c0d8a",hash(migrations.resolve("20260827000100_create_database_baseline.sql")));
        assertEquals("6bd5fb3595e8dcf5f7cc373854a3db22b37aa41014cbec71633f8e270164935a",hash(migrations.resolve("20260828000100_add_user_strategies_and_jobs.sql")));
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

package com.cryptostrategy.platform.persistence.internal.experiment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StandaloneBacktestMigrationContractTest {
    private final Path migration = Path.of(System.getProperty("user.dir"))
            .resolve("../..")
            .normalize()
            .resolve("supabase/migrations/20260902000100_f009_standalone_backtest.sql");

    @Test
    void createsForwardOnlyOwnerScopedGraphConstraintsAndNoBrowserGrant() throws Exception {
        String sql = Files.readString(migration);

        assertThat(sql).contains(
                "begin;",
                "create table experiment.standalone_backtest",
                "standalone_backtest_candidate_experiment_fk",
                "standalone_backtest_job_candidate_fk",
                "standalone_backtest_public_identity_distinct",
                "revoke all on table experiment.standalone_backtest from anon, authenticated",
                "commit;");
        assertThat(sql).doesNotContain(
                "drop table experiment.standalone_backtest",
                "grant insert",
                "grant update");
    }

    @Test
    void persistenceSqlKeepsReceiptAndEntireGraphInsideOneAdapterBoundary() {
        assertThat(ExperimentSql.INSERT_STANDALONE_BACKTEST)
                .contains("INSERT INTO experiment.standalone_backtest");
        assertThat(ExperimentSql.SELECT_STANDALONE_BACKTEST_BY_ID)
                .contains("JOIN experiment.experiment")
                .contains("owner_user_id = ?");
        assertThat(ExperimentSql.INSERT_IDEMPOTENCY_CLAIM)
                .contains("ON CONFLICT (user_id, scope, idempotency_key) DO NOTHING");
        assertThat(ExperimentSql.COMPLETE_IDEMPOTENCY_RECORD)
                .contains("state = 'COMPLETED'");
    }
}

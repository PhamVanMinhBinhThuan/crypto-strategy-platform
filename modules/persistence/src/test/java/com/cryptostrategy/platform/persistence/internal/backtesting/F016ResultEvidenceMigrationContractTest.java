package com.cryptostrategy.platform.persistence.internal.backtesting;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class F016ResultEvidenceMigrationContractTest {
    @Test
    void addsImmutableResultOwnedDecisionEvidenceAsAJsonArray() throws Exception {
        Path migration = Path.of(System.getProperty("user.dir")).resolve("../..").normalize()
                .resolve("supabase/migrations/20260911000100_f016_result_decision_evidence.sql");
        String sql = Files.readString(migration).toLowerCase();

        assertThat(sql).contains("alter table experiment.backtest_result")
                .contains("strategy_decision_evidence jsonb not null")
                .contains("jsonb_typeof(strategy_decision_evidence) = 'array'");
    }
}

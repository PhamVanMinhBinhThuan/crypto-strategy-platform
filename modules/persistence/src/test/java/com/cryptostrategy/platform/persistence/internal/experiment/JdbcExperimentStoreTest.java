package com.cryptostrategy.platform.persistence.internal.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcExperimentStoreTest {

    @Test
    @DisplayName("Experiment SQL contains expected insert and freeze statements")
    void experimentSqlStructure() {
        assertThat(ExperimentSql.INSERT_EXPERIMENT).contains("INSERT INTO experiment.experiment");
        assertThat(ExperimentSql.INSERT_MANIFEST).contains("INSERT INTO experiment.experiment_manifest");
        assertThat(ExperimentSql.FREEZE_AND_QUEUE_EXPERIMENT).contains("status = 'QUEUED'");
        assertThat(ExperimentSql.UPDATE_MANIFEST_FINGERPRINT).contains("SET fingerprint = ?");
    }
}

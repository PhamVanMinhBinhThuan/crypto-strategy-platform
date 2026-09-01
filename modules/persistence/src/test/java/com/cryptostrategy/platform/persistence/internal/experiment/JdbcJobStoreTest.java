package com.cryptostrategy.platform.persistence.internal.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcJobStoreTest {

    @Test
    @DisplayName("Job SQL uses row locking on parent Job and MAX(attempt_no) for sequential allocation")
    void jobLockingSqlStructure() {
        assertThat(ExperimentSql.SELECT_JOB_BY_ID_FOR_UPDATE).contains("FOR UPDATE OF j");
        assertThat(ExperimentSql.SELECT_MAX_ATTEMPT_NO).contains("coalesce(max(attempt_no), 0)");
        assertThat(ExperimentSql.INSERT_ATTEMPT).contains("INSERT INTO experiment.execution_attempt");
    }
}

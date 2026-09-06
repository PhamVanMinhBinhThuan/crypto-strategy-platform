package com.cryptostrategy.platform.persistence.internal.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobRecoveryQueryTest {

    @Test
    @DisplayName("Unfinished jobs query filters for non-terminal Job statuses")
    void unfinishedJobsQuery() {
        assertThat(ExperimentSql.SELECT_UNFINISHED_JOBS).contains("status IN ('QUEUED', 'RUNNING', 'RETRY_SCHEDULED', 'CANCEL_REQUESTED')");
    }

    @Test
    @DisplayName("Stale attempt recovery only selects attempts whose parent Job is still running")
    void staleAttemptsRequireRunningJob() {
        assertThat(ExperimentSql.SELECT_STALE_RUNNING_ATTEMPTS)
                .contains("ea.status = 'RUNNING' AND j.status = 'RUNNING'");
    }
}

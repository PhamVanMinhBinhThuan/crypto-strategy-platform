package com.cryptostrategy.platform.persistence.internal.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OwnershipSqlPredicateTest {

    @Test
    @DisplayName("Every private Experiment query enforces owner_user_id predicate")
    void experimentQueriesHaveOwnerPredicate() {
        assertThat(ExperimentSql.SELECT_EXPERIMENT_BY_ID).contains("owner_user_id = ?");
        assertThat(ExperimentSql.SELECT_MANIFEST_BY_EXPERIMENT_ID).contains("owner_user_id = ?");
        assertThat(ExperimentSql.FREEZE_AND_QUEUE_EXPERIMENT).contains("owner_user_id = ?");
        assertThat(ExperimentSql.UPDATE_EXPERIMENT_STATUS).contains("owner_user_id = ?");
    }

    @Test
    @DisplayName("Every private Job and Candidate query joins to experiment and enforces owner_user_id")
    void jobAndCandidateQueriesHaveOwnerPredicate() {
        assertThat(ExperimentSql.SELECT_JOB_BY_ID).contains("owner_user_id = ?");
        assertThat(ExperimentSql.SELECT_JOB_BY_ID_FOR_UPDATE).contains("owner_user_id = ?");
        assertThat(ExperimentSql.SELECT_BACKTEST_JOB_BY_CANDIDATE).contains("owner_user_id = ?");
        assertThat(ExperimentSql.SELECT_CANDIDATE_BY_ID).contains("owner_user_id = ?");
        assertThat(ExperimentSql.SELECT_CANDIDATES_BY_EXPERIMENT_ID).contains("owner_user_id = ?");
        assertThat(ExperimentSql.SELECT_ATTEMPTS_BY_JOB_ID).contains("owner_user_id = ?");
    }
}

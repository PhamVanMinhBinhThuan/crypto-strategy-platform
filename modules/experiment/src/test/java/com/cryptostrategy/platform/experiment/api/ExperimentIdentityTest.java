package com.cryptostrategy.platform.experiment.api;

import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.WorkerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExperimentIdentityTest {

    private static final String VALID_ULID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String VALID_ULID_2 = "01ARZ3NDEKTSV4RRFFQ69G5FAW";

    @Test
    @DisplayName("ExperimentId validates Crockford ULID and supports generation")
    void experimentIdValidation() {
        ExperimentId id = new ExperimentId(VALID_ULID);
        assertThat(id.value()).isEqualTo(VALID_ULID);
        assertThat(id.toString()).isEqualTo(VALID_ULID);

        ExperimentId generated = ExperimentId.generate();
        assertThat(generated.value()).hasSize(26).matches("^[0-9A-HJKMNP-TV-Z]{26}$");

        assertThatThrownBy(() -> new ExperimentId("invalid-ulid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CandidateId validates Crockford ULID and supports generation")
    void candidateIdValidation() {
        CandidateId id = new CandidateId(VALID_ULID);
        assertThat(id.value()).isEqualTo(VALID_ULID);
        assertThat(id.toString()).isEqualTo(VALID_ULID);

        CandidateId generated = CandidateId.generate();
        assertThat(generated.value()).hasSize(26).matches("^[0-9A-HJKMNP-TV-Z]{26}$");

        assertThatThrownBy(() -> new CandidateId("invalid-ulid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("JobId validates Crockford ULID and supports generation")
    void jobIdValidation() {
        JobId id = new JobId(VALID_ULID);
        assertThat(id.value()).isEqualTo(VALID_ULID);
        assertThat(id.toString()).isEqualTo(VALID_ULID);

        JobId generated = JobId.generate();
        assertThat(generated.value()).hasSize(26).matches("^[0-9A-HJKMNP-TV-Z]{26}$");

        assertThatThrownBy(() -> new JobId("invalid-ulid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("AttemptId validates Crockford ULID and supports generation")
    void attemptIdValidation() {
        AttemptId id = new AttemptId(VALID_ULID);
        assertThat(id.value()).isEqualTo(VALID_ULID);
        assertThat(id.toString()).isEqualTo(VALID_ULID);

        AttemptId generated = AttemptId.generate();
        assertThat(generated.value()).hasSize(26).matches("^[0-9A-HJKMNP-TV-Z]{26}$");

        assertThatThrownBy(() -> new AttemptId("invalid-ulid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("WorkerId validates non-empty string identifier")
    void workerIdValidation() {
        WorkerId id = new WorkerId("worker-node-1");
        assertThat(id.value()).isEqualTo("worker-node-1");
        assertThat(id.toString()).isEqualTo("worker-node-1");

        assertThatThrownBy(() -> new WorkerId(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WorkerId("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WorkerId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("ULID wrappers obey equality and distinct typing")
    void equalityAndTyping() {
        ExperimentId exp1 = new ExperimentId(VALID_ULID);
        ExperimentId exp2 = new ExperimentId(VALID_ULID);
        ExperimentId expDiff = new ExperimentId(VALID_ULID_2);
        JobId job1 = new JobId(VALID_ULID);

        assertThat(exp1).isEqualTo(exp2).hasSameHashCodeAs(exp2);
        assertThat(exp1).isNotEqualTo(expDiff);
        assertThat(exp1).isNotEqualTo(job1);
    }
}

package com.cryptostrategy.platform.experiment.api;

import com.cryptostrategy.platform.experiment.api.job.AttemptStatus;
import com.cryptostrategy.platform.experiment.api.job.FailureClassification;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import com.cryptostrategy.platform.experiment.api.job.JobType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExperimentEnumsTest {

    @Test
    @DisplayName("ExperimentStatus contains only canonical lifecycle values")
    void experimentStatuses() {
        assertThat(ExperimentStatus.values()).containsExactly(
                ExperimentStatus.CREATED,
                ExperimentStatus.QUEUED,
                ExperimentStatus.RUNNING,
                ExperimentStatus.COMPLETED,
                ExperimentStatus.FAILED,
                ExperimentStatus.STOP_REQUESTED,
                ExperimentStatus.STOPPED
        );

        assertThat(ExperimentStatus.CREATED.isMutable()).isTrue();
        assertThat(ExperimentStatus.QUEUED.isMutable()).isFalse();
        assertThat(ExperimentStatus.RUNNING.isTerminal()).isFalse();
        assertThat(ExperimentStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(ExperimentStatus.FAILED.isTerminal()).isTrue();
        assertThat(ExperimentStatus.STOPPED.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("JobType defines SEARCH and BACKTEST")
    void jobTypes() {
        assertThat(JobType.values()).containsExactly(
                JobType.SEARCH,
                JobType.BACKTEST
        );
    }

    @Test
    @DisplayName("JobStatus defines canonical job state machine")
    void jobStatuses() {
        assertThat(JobStatus.values()).containsExactly(
                JobStatus.QUEUED,
                JobStatus.RUNNING,
                JobStatus.RETRY_SCHEDULED,
                JobStatus.SUCCEEDED,
                JobStatus.FAILED,
                JobStatus.CANCEL_REQUESTED,
                JobStatus.CANCELLED
        );

        assertThat(JobStatus.SUCCEEDED.isTerminal()).isTrue();
        assertThat(JobStatus.FAILED.isTerminal()).isTrue();
        assertThat(JobStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(JobStatus.RUNNING.isTerminal()).isFalse();
        assertThat(JobStatus.RETRY_SCHEDULED.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("AttemptStatus defines terminal-only tries without RETRY_SCHEDULED")
    void attemptStatuses() {
        assertThat(AttemptStatus.values()).containsExactly(
                AttemptStatus.QUEUED,
                AttemptStatus.RUNNING,
                AttemptStatus.SUCCEEDED,
                AttemptStatus.FAILED,
                AttemptStatus.CANCELLED
        );

        assertThat(AttemptStatus.SUCCEEDED.isTerminal()).isTrue();
        assertThat(AttemptStatus.FAILED.isTerminal()).isTrue();
        assertThat(AttemptStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(AttemptStatus.RUNNING.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("FailureClassification defines transient vs deterministic failures")
    void failureClassifications() {
        assertThat(FailureClassification.values()).containsExactly(
                FailureClassification.TRANSIENT,
                FailureClassification.DETERMINISTIC
        );
        assertThat(FailureClassification.TRANSIENT.isRetryable()).isTrue();
        assertThat(FailureClassification.DETERMINISTIC.isRetryable()).isFalse();
    }
}

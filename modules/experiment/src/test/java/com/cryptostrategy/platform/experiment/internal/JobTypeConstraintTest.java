package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobTypeConstraintTest {

    private final ExperimentId experimentId = ExperimentId.generate();
    private final CandidateId candidateId = CandidateId.generate();
    private final JobId jobId = JobId.generate();

    @Test
    @DisplayName("Search Job must have null candidateId")
    void searchJobConstraint() {
        Job searchJob = Job.createSearchJob(jobId, experimentId, "corr-search", 10, Instant.now());
        assertThat(searchJob.jobType()).isEqualTo(JobType.SEARCH);
        assertThat(searchJob.candidateId()).isNull();
        assertThat(searchJob.totalWork()).isEqualTo(10);

        assertThatThrownBy(() -> new Job(jobId, experimentId, candidateId, JobType.SEARCH, searchJob.status(), "corr", 10, 0, 0, null, null, null, null, null, null, null, Instant.now(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Backtest Job must have non-null candidateId")
    void backtestJobConstraint() {
        Job backtestJob = Job.createBacktestJob(jobId, experimentId, candidateId, "corr-backtest", Instant.now());
        assertThat(backtestJob.jobType()).isEqualTo(JobType.BACKTEST);
        assertThat(backtestJob.candidateId()).isEqualTo(candidateId);
        assertThat(backtestJob.totalWork()).isEqualTo(1);

        assertThatThrownBy(() -> new Job(jobId, experimentId, null, JobType.BACKTEST, backtestJob.status(), "corr", 1, 0, 0, null, null, null, null, null, null, null, Instant.now(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

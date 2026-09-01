package com.cryptostrategy.platform.experiment.api.port.out;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobStore {
    void insertJob(UUID ownerUserId, Job job, OutboxEvent outboxEvent);
    Optional<Job> findJobById(UUID ownerUserId, JobId jobId);
    Optional<Job> findBacktestJobByCandidateId(UUID ownerUserId, CandidateId candidateId);
    List<Job> listJobsByExperimentId(UUID ownerUserId, ExperimentId experimentId);
    List<Job> listUnfinishedJobs();
    void updateJobStatus(UUID ownerUserId, JobId jobId, JobStatus newStatus, Instant updatedAt);
    void cancelJobWithOutbox(UUID ownerUserId, JobId jobId, JobStatus newStatus, OutboxEvent outboxEvent, Instant updatedAt);
    void cancelJobWithoutOutbox(UUID ownerUserId, JobId jobId, JobStatus newStatus, Instant updatedAt);
    void requeueRetryWithOutbox(UUID ownerUserId, JobId jobId, OutboxEvent outboxEvent, Instant queuedAt);
}

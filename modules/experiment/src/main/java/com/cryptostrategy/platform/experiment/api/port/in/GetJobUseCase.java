package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Owner-scoped authoritative reads for durable Jobs. */
public interface GetJobUseCase {
    Optional<Job> getJob(UUID ownerUserId, JobId jobId);

    List<Job> listJobs(UUID ownerUserId, ExperimentId experimentId);
}

package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.job.JobId;

import java.util.UUID;

public interface CancelJobUseCase {
    void cancelJob(UUID ownerUserId, JobId jobId);
    boolean isCancelRequested(UUID ownerUserId, JobId jobId);
}

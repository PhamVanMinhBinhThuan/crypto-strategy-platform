package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.job.JobId;

import java.util.UUID;

public interface RequeueRetryUseCase {
    void requeueRetry(UUID ownerUserId, JobId jobId);
}

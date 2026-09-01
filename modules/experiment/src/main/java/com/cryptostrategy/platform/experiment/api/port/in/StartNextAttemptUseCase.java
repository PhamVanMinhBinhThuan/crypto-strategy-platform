package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.JobId;

import java.util.UUID;

public interface StartNextAttemptUseCase {
    ExecutionAttempt startNextAttempt(UUID ownerUserId, JobId jobId, String workerId);
}

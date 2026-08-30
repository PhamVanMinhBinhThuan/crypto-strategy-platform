package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.FailureClassification;
import com.cryptostrategy.platform.experiment.api.job.JobId;

import java.time.Instant;
import java.util.UUID;

public interface FinalizeAttemptUseCase {
    void finalizeSuccess(UUID ownerUserId, JobId jobId, AttemptId attemptId);
    void finalizeFailure(UUID ownerUserId, JobId jobId, AttemptId attemptId, String failureCode, String failureMessage, FailureClassification classification, Instant nextRetryAt);
    void finalizeCancelled(UUID ownerUserId, JobId jobId, AttemptId attemptId);
}

package com.cryptostrategy.platform.experiment.api.port.out;

import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.JobId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ExecutionAttemptStore {
    ExecutionAttempt startNextAttempt(UUID ownerUserId, JobId jobId, String workerId, Instant startTime);
    void finalizeAttemptSuccess(UUID ownerUserId, JobId jobId, AttemptId attemptId, Instant finishTime);
    void finalizeAttemptRetryableFailure(UUID ownerUserId, JobId jobId, AttemptId attemptId, String failureCode, String failureMessage, Instant finishTime, Instant nextRetryTime);
    void finalizeAttemptTerminalFailure(UUID ownerUserId, JobId jobId, AttemptId attemptId, String failureCode, String failureMessage, Instant finishTime);
    void finalizeAttemptCancelled(UUID ownerUserId, JobId jobId, AttemptId attemptId, Instant finishTime);
    List<ExecutionAttempt> listAttemptsByJobId(UUID ownerUserId, JobId jobId);
}

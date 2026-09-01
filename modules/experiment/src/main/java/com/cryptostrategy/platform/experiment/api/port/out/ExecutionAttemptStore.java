package com.cryptostrategy.platform.experiment.api.port.out;

import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.StaleRunningAttempt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ExecutionAttemptStore {
    ExecutionAttempt startNextAttempt(UUID ownerUserId, JobId jobId, String workerId, Instant startTime);

    /**
     * Conditionally updates Attempt and Job to SUCCEEDED only if Attempt is RUNNING.
     * @return true if updated, false if state conflict/already terminal
     */
    boolean finalizeAttemptSuccess(UUID ownerUserId, JobId jobId, AttemptId attemptId, Instant finishTime);

    /**
     * Conditionally updates Attempt to FAILED and Job to RETRY_SCHEDULED only if Attempt is RUNNING.
     * @return true if updated, false if state conflict/already terminal
     */
    boolean finalizeAttemptRetryableFailure(UUID ownerUserId, JobId jobId, AttemptId attemptId, String failureCode, String failureMessage, Instant finishTime, Instant nextRetryTime);

    /**
     * Conditionally updates Attempt to FAILED and Job to FAILED only if Attempt is RUNNING.
     * @return true if updated, false if state conflict/already terminal
     */
    boolean finalizeAttemptTerminalFailure(UUID ownerUserId, JobId jobId, AttemptId attemptId, String failureCode, String failureMessage, Instant finishTime);

    /**
     * Conditionally updates Attempt to CANCELLED and Job to CANCELLED only if Attempt is RUNNING (or CANCEL_REQUESTED).
     * @return true if updated, false if state conflict/already terminal
     */
    boolean finalizeAttemptCancelled(UUID ownerUserId, JobId jobId, AttemptId attemptId, Instant finishTime);

    List<ExecutionAttempt> listAttemptsByJobId(UUID ownerUserId, JobId jobId);

    List<StaleRunningAttempt> findStaleRunningAttempts(Instant startedBefore, int limit);
}

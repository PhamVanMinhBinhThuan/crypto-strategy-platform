package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.job.DueRetryJob;
import com.cryptostrategy.platform.experiment.api.job.RecoverableQueuedJob;
import com.cryptostrategy.platform.experiment.api.job.StaleRunningAttempt;
import com.cryptostrategy.platform.experiment.api.job.StopCandidateExperiment;

import java.time.Instant;
import java.util.List;

public interface TrustedWorkerRecoveryQueryUseCase {

    List<RecoverableQueuedJob> findRecoverableQueuedJobs(Instant olderThan, int limit);

    List<DueRetryJob> findDueRetries(Instant dueAtOrBefore, int limit);

    List<StaleRunningAttempt> findStaleRunningAttempts(Instant startedBefore, int limit);

    List<StopCandidateExperiment> findStopCompletionCandidates(int limit);
}

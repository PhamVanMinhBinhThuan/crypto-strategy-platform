package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.execution.FrozenBacktestExecution;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.FailureClassification;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.TerminalWorkOutcome;
import com.cryptostrategy.platform.experiment.api.job.WorkerId;

import java.math.BigDecimal;
import java.time.Instant;

public interface TrustedWorkerExperimentUseCase {

    ExecutionAttempt startNextAttempt(JobId jobId, WorkerId workerId);

    FrozenBacktestExecution getFrozenExecution(JobId jobId);

    void finalizeSuccess(JobId jobId, AttemptId attemptId);

    void finalizeFailure(
            JobId jobId,
            AttemptId attemptId,
            String failureCode,
            String failureMessage,
            FailureClassification classification,
            Instant nextRetryAt
    );

    void finalizeCancelled(JobId jobId, AttemptId attemptId);

    boolean isCancelRequested(JobId jobId);

    void requeueDueRetry(JobId jobId);

    Job getJob(JobId jobId);

    ExperimentStatus getExperimentStatus(ExperimentId experimentId);

    void recordTerminalProgress(
            JobId jobId,
            TerminalWorkOutcome outcome,
            BigDecimal score
    );
}

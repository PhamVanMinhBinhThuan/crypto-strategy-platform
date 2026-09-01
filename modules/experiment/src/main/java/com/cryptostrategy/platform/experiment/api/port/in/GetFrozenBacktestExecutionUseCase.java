package com.cryptostrategy.platform.experiment.api.port.in;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.execution.FrozenBacktestExecution;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.util.UUID;

public interface GetFrozenBacktestExecutionUseCase {
    FrozenBacktestExecution getFrozenExecution(
            UUID ownerUserId,
            ExperimentId experimentId,
            CandidateId candidateId,
            JobId jobId,
            AttemptId attemptId
    );
}

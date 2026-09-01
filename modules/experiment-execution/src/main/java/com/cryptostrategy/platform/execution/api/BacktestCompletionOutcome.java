package com.cryptostrategy.platform.execution.api;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;

import java.math.BigDecimal;
import java.util.Objects;

public record BacktestCompletionOutcome(
        ExperimentId experimentId,
        JobId jobId,
        CandidateId candidateId,
        BacktestResultId backtestResultId,
        EvaluationResultId evaluationResultId,
        BigDecimal overallScore
) {
    public BacktestCompletionOutcome {
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(candidateId, "candidateId cannot be null");
        Objects.requireNonNull(backtestResultId, "backtestResultId cannot be null");
        Objects.requireNonNull(evaluationResultId, "evaluationResultId cannot be null");
        Objects.requireNonNull(overallScore, "overallScore cannot be null");
    }
}

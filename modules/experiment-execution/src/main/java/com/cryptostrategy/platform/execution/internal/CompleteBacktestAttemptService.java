package com.cryptostrategy.platform.execution.internal;

import com.cryptostrategy.platform.backtesting.api.PreparedBacktestOutcome;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.port.in.CommitPreparedBacktestUseCase;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.evaluation.api.model.MetricVersion;
import com.cryptostrategy.platform.evaluation.api.model.RankingVersion;
import com.cryptostrategy.platform.evaluation.api.port.in.EvaluateBacktestUseCase;
import com.cryptostrategy.platform.execution.api.BacktestCompletionOutcome;
import com.cryptostrategy.platform.execution.api.port.in.CompleteBacktestAttemptUseCase;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.TerminalWorkOutcome;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

public class CompleteBacktestAttemptService implements CompleteBacktestAttemptUseCase {

    private final TrustedWorkerExperimentUseCase experimentUseCase;
    private final CommitPreparedBacktestUseCase commitBacktestUseCase;
    private final EvaluateBacktestUseCase evaluateBacktestUseCase;
    private final TransactionTemplate transactionTemplate;

    public CompleteBacktestAttemptService(
            TrustedWorkerExperimentUseCase experimentUseCase,
            CommitPreparedBacktestUseCase commitBacktestUseCase,
            EvaluateBacktestUseCase evaluateBacktestUseCase,
            TransactionTemplate transactionTemplate
    ) {
        this.experimentUseCase = Objects.requireNonNull(experimentUseCase, "experimentUseCase cannot be null");
        this.commitBacktestUseCase = Objects.requireNonNull(commitBacktestUseCase, "commitBacktestUseCase cannot be null");
        this.evaluateBacktestUseCase = Objects.requireNonNull(evaluateBacktestUseCase, "evaluateBacktestUseCase cannot be null");
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public BacktestCompletionOutcome completeAttempt(
            JobId jobId,
            AttemptId attemptId,
            PreparedBacktestOutcome preparedOutcome
    ) {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(attemptId, "attemptId cannot be null");
        Objects.requireNonNull(preparedOutcome, "preparedOutcome cannot be null");

        if (transactionTemplate != null) {
            return transactionTemplate.execute(status -> executeCompletion(jobId, attemptId, preparedOutcome));
        } else {
            return executeCompletion(jobId, attemptId, preparedOutcome);
        }
    }

    private BacktestCompletionOutcome executeCompletion(
            JobId jobId,
            AttemptId attemptId,
            PreparedBacktestOutcome preparedOutcome
    ) {
        // 1. Finalize attempt to SUCCEEDED (CAS guard: attempt=RUNNING)
        experimentUseCase.finalizeSuccess(jobId, attemptId);

        // 2. Persist BacktestResult with SUCCEEDED attempt lineage
        BacktestResult savedResult = commitBacktestUseCase.commit(preparedOutcome);

        // 3. Evaluate the BacktestResult
        MetricVersion metricVersion = new MetricVersion("metric-v1");
        RankingVersion rankingVersion = new RankingVersion("ranking-v1");
        EvaluationResult evaluation = evaluateBacktestUseCase.evaluate(savedResult, metricVersion, rankingVersion);

        // 4. Idempotently record terminal progress (completed_work=1, best_score updated)
        experimentUseCase.recordTerminalProgress(jobId, TerminalWorkOutcome.SUCCEEDED, evaluation.overallScore());

        return new BacktestCompletionOutcome(
                savedResult.experimentId(),
                jobId,
                savedResult.candidateId(),
                savedResult.resultId(),
                evaluation.evaluationResultId(),
                evaluation.overallScore()
        );
    }
}

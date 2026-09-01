package com.cryptostrategy.platform.execution.internal;

import com.cryptostrategy.platform.backtesting.api.PreparedBacktestOutcome;
import com.cryptostrategy.platform.backtesting.api.model.BacktestAssumptions;
import com.cryptostrategy.platform.backtesting.api.model.BacktestProvenance;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.backtesting.api.model.EquityCurveSummary;
import com.cryptostrategy.platform.backtesting.api.model.Money;
import com.cryptostrategy.platform.backtesting.api.port.in.CommitPreparedBacktestUseCase;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import com.cryptostrategy.platform.evaluation.api.model.MetricVersion;
import com.cryptostrategy.platform.evaluation.api.model.RankingVersion;
import com.cryptostrategy.platform.evaluation.api.port.in.EvaluateBacktestUseCase;
import com.cryptostrategy.platform.execution.api.BacktestCompletionOutcome;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.TerminalWorkOutcome;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompleteBacktestAttemptServiceTest {

    private TrustedWorkerExperimentUseCase experimentUseCase;
    private CommitPreparedBacktestUseCase commitBacktestUseCase;
    private EvaluateBacktestUseCase evaluateBacktestUseCase;
    private CompleteBacktestAttemptService service;

    private final ExperimentId experimentId = ExperimentId.generate();
    private final CandidateId candidateId = CandidateId.generate();
    private final JobId jobId = JobId.generate();
    private final AttemptId attemptId = AttemptId.generate();
    private final BacktestResultId resultId = BacktestResultId.generate();
    private final EvaluationResultId evalId = EvaluationResultId.generate();

    @BeforeEach
    void setUp() {
        experimentUseCase = mock(TrustedWorkerExperimentUseCase.class);
        commitBacktestUseCase = mock(CommitPreparedBacktestUseCase.class);
        evaluateBacktestUseCase = mock(EvaluateBacktestUseCase.class);

        service = new CompleteBacktestAttemptService(
                experimentUseCase,
                commitBacktestUseCase,
                evaluateBacktestUseCase,
                null
        );
    }

    @Test
    void completesAttemptAtomicallyWithSuccessLineageAndProgress() {
        Instant now = Instant.now();
        BacktestAssumptions assumptions = BacktestAssumptions.mvp(BigDecimal.valueOf(10000), BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.0005));
        EquityCurveSummary summary = new EquityCurveSummary(100L, Money.of(BigDecimal.valueOf(12000)), Money.of(BigDecimal.valueOf(9500)), 10L, 50L, "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

        BacktestResult result = new BacktestResult(
                resultId, experimentId, candidateId, jobId, attemptId,
                new BacktestProvenance("fp", "ds-cs", "st-fp"),
                assumptions, Money.of(BigDecimal.valueOf(10000)),
                Money.of(BigDecimal.valueOf(12000)), Money.of(BigDecimal.ZERO),
                List.of(), summary,
                "result-fp", now
        );
        PreparedBacktestOutcome prepared = new PreparedBacktestOutcome(result, List.of());

        when(commitBacktestUseCase.commit(prepared)).thenReturn(result);

        EvaluationResult eval = new EvaluationResult(
                evalId, experimentId, resultId, new MetricVersion("metric-v1"), new RankingVersion("ranking-v1"),
                BigDecimal.valueOf(0.20), BigDecimal.valueOf(0.60), BigDecimal.valueOf(0.05), 10,
                BigDecimal.valueOf(0.85), true, "eval-fp", now
        );
        when(evaluateBacktestUseCase.evaluate(eq(result), any(), any())).thenReturn(eval);

        BacktestCompletionOutcome outcome = service.completeAttempt(jobId, attemptId, prepared);

        assertThat(outcome.experimentId()).isEqualTo(experimentId);
        assertThat(outcome.jobId()).isEqualTo(jobId);
        assertThat(outcome.candidateId()).isEqualTo(candidateId);
        assertThat(outcome.backtestResultId()).isEqualTo(resultId);
        assertThat(outcome.evaluationResultId()).isEqualTo(evalId);
        assertThat(outcome.overallScore()).isEqualTo(eval.overallScore());

        verify(experimentUseCase).finalizeSuccess(jobId, attemptId);
        verify(commitBacktestUseCase).commit(prepared);
        verify(evaluateBacktestUseCase).evaluate(eq(result), any(), any());
        verify(experimentUseCase).recordTerminalProgress(jobId, TerminalWorkOutcome.SUCCEEDED, eval.overallScore());
    }
}

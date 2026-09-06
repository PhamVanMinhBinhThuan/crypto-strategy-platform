package com.cryptostrategy.platform.leaderboard.api.model;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Authoritative read projection for one candidate across backtest, evaluation and ranking. */
public record CandidatePipelineEntry(
        ExperimentId experimentId, CandidateId candidateId, int generationIndex,
        Map<String, Object> definition, String candidateFingerprint, Instant createdAt,
        BacktestStage backtest, EvaluationStage evaluation, RankingStage ranking,
        String failureStage) {
    public CandidatePipelineEntry {
        Objects.requireNonNull(experimentId);
        Objects.requireNonNull(candidateId);
        definition = Map.copyOf(Objects.requireNonNull(definition));
        Objects.requireNonNull(candidateFingerprint);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(backtest);
        Objects.requireNonNull(evaluation);
        Objects.requireNonNull(ranking);
    }

    public record Failure(String code, String message) {}
    public record BacktestStage(JobId jobId, String status, LeaderboardBacktestResultId backtestResultId,
            Instant startedAt, Instant finishedAt, Integer attemptNo, Instant nextRetryAt,
            boolean retryable, Failure failure) {}
    public record EvaluationStage(String status, EvaluationResultId evaluationResultId, BigDecimal score,
            BigDecimal totalReturn, BigDecimal winRate, BigDecimal maximumDrawdown,
            Integer numberOfTrades, String metricVersion, Boolean eligible,
            EligibilityReason eligibilityReason, Instant evaluatedAt) {}
    public record RankingStage(String status, Integer rank, String rankingVersion) {}
    public record EligibilityReason(String code, Integer actualTrades, Integer requiredTrades) {}
}

package com.cryptostrategy.platform.backtesting.api.model;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Lightweight owner-scoped projection for backtest result history. */
public record BacktestResultSummary(
        BacktestResultId backtestResultId,
        ExperimentId experimentId,
        CandidateId candidateId,
        int generationIndex,
        String experimentName,
        Map<String, Object> candidateDefinition,
        BigDecimal totalReturn,
        BigDecimal winRate,
        BigDecimal maximumDrawdown,
        int numberOfTrades,
        BigDecimal score,
        Instant completedAt) {
    public BacktestResultSummary {
        Objects.requireNonNull(backtestResultId, "backtestResultId");
        Objects.requireNonNull(experimentId, "experimentId");
        Objects.requireNonNull(candidateId, "candidateId");
        Objects.requireNonNull(experimentName, "experimentName");
        candidateDefinition = Map.copyOf(candidateDefinition);
        Objects.requireNonNull(totalReturn, "totalReturn");
        Objects.requireNonNull(winRate, "winRate");
        Objects.requireNonNull(maximumDrawdown, "maximumDrawdown");
        Objects.requireNonNull(completedAt, "completedAt");
    }
}

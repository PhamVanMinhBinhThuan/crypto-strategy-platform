package com.cryptostrategy.platform.backtesting.api.model;
import com.cryptostrategy.platform.experiment.api.*;
import com.cryptostrategy.platform.experiment.api.job.*;
import java.time.Instant;
import java.util.*;
public record BacktestResult(BacktestResultId resultId,ExperimentId experimentId,CandidateId candidateId,JobId jobId,
        AttemptId successfulAttemptId,BacktestProvenance provenance,BacktestAssumptions assumptions,Money initialCapital,
        Money finalCapital,Money totalFees,List<Trade> trades,EquityCurveSummary equityCurveSummary,String fingerprint,Instant completedAt) {
    public BacktestResult { Objects.requireNonNull(resultId);Objects.requireNonNull(experimentId);Objects.requireNonNull(candidateId);Objects.requireNonNull(jobId);Objects.requireNonNull(successfulAttemptId);Objects.requireNonNull(provenance);Objects.requireNonNull(assumptions);Objects.requireNonNull(initialCapital);Objects.requireNonNull(finalCapital);Objects.requireNonNull(totalFees);trades=List.copyOf(trades);Objects.requireNonNull(equityCurveSummary);Objects.requireNonNull(fingerprint);Objects.requireNonNull(completedAt);if(fingerprint.isBlank())throw new IllegalArgumentException("fingerprint");for(int i=0;i<trades.size();i++)if(trades.get(i).sequence()!=i||!trades.get(i).backtestResultId().equals(resultId))throw new IllegalArgumentException("Trade sequence/result mismatch"); }
}

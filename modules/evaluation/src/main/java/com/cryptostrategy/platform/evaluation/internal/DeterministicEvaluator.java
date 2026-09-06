package com.cryptostrategy.platform.evaluation.internal;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationEligibilityPolicy;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import com.cryptostrategy.platform.evaluation.api.model.MetricVersion;
import com.cryptostrategy.platform.evaluation.api.model.NormalizedMetrics;
import com.cryptostrategy.platform.evaluation.api.model.RankingVersion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public final class DeterministicEvaluator {
    private final MetricCalculator calculator = new MetricCalculator();

    public EvaluationResult evaluate(
            BacktestResult result,
            MetricVersion metricVersion,
            RankingVersion rankingVersion
    ) {
        var metrics = calculator.calculate(result);
        BigDecimal returnScore = clamp(metrics.totalReturn());
        BigDecimal winScore = clamp(metrics.winRate());
        BigDecimal drawdownScore = BigDecimal.ONE.setScale(10)
                .subtract(clamp(metrics.maximumDrawdown()));
        BigDecimal overall = returnScore.multiply(new BigDecimal("0.45"))
                .add(winScore.multiply(new BigDecimal("0.30")))
                .add(drawdownScore.multiply(new BigDecimal("0.25")))
                .setScale(10, RoundingMode.HALF_EVEN);
        var normalized = new NormalizedMetrics(
                returnScore, winScore, drawdownScore, overall);
        String fingerprint = new EvaluationFingerprintV1().calculate(
                result.fingerprint(), metricVersion, rankingVersion, metrics, normalized);
        return new EvaluationResult(
                EvaluationResultId.generate(), result.experimentId(), result.resultId(),
                metricVersion, rankingVersion, metrics.totalReturn(), metrics.winRate(),
                metrics.maximumDrawdown(), metrics.numberOfTrades(), overall,
                EvaluationEligibilityPolicy.isEligible(metrics.numberOfTrades()),
                fingerprint, Instant.now());
    }

    private static BigDecimal clamp(BigDecimal value) {
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE)
                .setScale(10, RoundingMode.HALF_EVEN);
    }
}

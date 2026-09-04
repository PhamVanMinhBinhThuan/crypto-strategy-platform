package com.cryptostrategy.platform.api.backtest;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.backtesting.api.model.Trade;
import com.cryptostrategy.platform.backtesting.api.model.TradeId;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyComponentSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.backtest.BacktestId;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.api.transport.TypedUlidSerializer;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ResultDtos {
    private ResultDtos() {}

    public record BacktestResultResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) BacktestResultId backtestResultId,
            @JsonSerialize(using = TypedUlidSerializer.class) BacktestId backtestId,
            String status,
            MetricsResponse metrics,
            List<TradeResponse> trades,
            ProvenanceResponse provenance,
            Map<String, Object> assumptions,
            String initialCapital,
            String finalCapital,
            String totalFees,
            Instant completedAt) {
        static BacktestResultResponse from(BacktestId backtestId, BacktestResult result) {
            return from(backtestId, result, null, null);
        }

        static BacktestResultResponse from(BacktestId backtestId, BacktestResult result,
                ExperimentManifest manifest, CandidateDefinition candidate) {
            return new BacktestResultResponse(
                    result.resultId(),
                    backtestId,
                    "COMPLETED",
                    MetricsResponse.from(result),
                    result.trades().stream().map(TradeResponse::from).toList(),
                    ProvenanceResponse.from(result, manifest, candidate),
                    ResultDtos.assumptions(result),
                    result.initialCapital().value().toPlainString(),
                    result.finalCapital().value().toPlainString(),
                    result.totalFees().value().toPlainString(),
                    result.completedAt());
        }
    }

    public record MetricsResponse(
            String totalReturn,
            String winRate,
            String maximumDrawdown,
            int numberOfTrades) {
        static MetricsResponse from(BacktestResult result) {
            BigDecimal initial = result.initialCapital().value();
            BigDecimal totalReturn = result.finalCapital().value()
                    .subtract(initial)
                    .divide(initial, 12, RoundingMode.HALF_EVEN);
            long wins = result.trades().stream()
                    .filter(trade -> trade.realizedPnl().signum() > 0)
                    .count();
            BigDecimal winRate = result.trades().isEmpty()
                    ? BigDecimal.ZERO.setScale(12)
                    : BigDecimal.valueOf(wins)
                            .divide(BigDecimal.valueOf(result.trades().size()), 12, RoundingMode.HALF_EVEN);
            BigDecimal peak = result.equityCurveSummary().peakEquity().value();
            BigDecimal drawdown = peak.signum() == 0
                    ? BigDecimal.ZERO.setScale(12)
                    : peak.subtract(result.equityCurveSummary().troughEquity().value())
                            .divide(peak, 12, RoundingMode.HALF_EVEN);
            return new MetricsResponse(
                    totalReturn.toPlainString(),
                    winRate.toPlainString(),
                    drawdown.toPlainString(),
                    result.trades().size());
        }
    }

    public record TradeResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) TradeId tradeId,
            int sequence,
            String side,
            Instant entryTime,
            String entryPrice,
            Instant exitTime,
            String exitPrice,
            String quantity,
            String entryFee,
            String exitFee,
            String totalFee,
            String profitLoss,
            String postTradeCash,
            String exitReason) {
        static TradeResponse from(Trade trade) {
            return new TradeResponse(
                    trade.tradeId(),
                    trade.sequence(),
                    trade.side().name(),
                    trade.entryTime(),
                    trade.entryPrice().value().toPlainString(),
                    trade.exitTime(),
                    trade.exitPrice().value().toPlainString(),
                    trade.quantity().value().toPlainString(),
                    trade.entryFee().value().toPlainString(),
                    trade.exitFee().value().toPlainString(),
                    trade.totalFee().value().toPlainString(),
                    trade.realizedPnl().toPlainString(),
                    trade.postTradeCash().value().toPlainString(),
                    trade.exitReason().name());
        }
    }

    public record ProvenanceResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId experimentId,
            @JsonSerialize(using = TypedUlidSerializer.class) CandidateId candidateId,
            @JsonSerialize(using = TypedUlidSerializer.class) JobId jobId,
            @JsonSerialize(using = TypedUlidSerializer.class) AttemptId successfulAttemptId,
            String manifestFingerprint,
            String datasetFingerprint,
            String strategyFingerprint,
            String resultFingerprint,
            String manifestVersion,
            DatasetEvidenceResponse dataset,
            StrategyEvidenceResponse strategy,
            CandidateEvidenceResponse candidate,
            String softwareVersion,
            String gitCommit) {
        static ProvenanceResponse from(BacktestResult result, ExperimentManifest manifest,
                CandidateDefinition candidate) {
            return new ProvenanceResponse(
                    result.experimentId(),
                    result.candidateId(),
                    result.jobId(),
                    result.successfulAttemptId(),
                    result.provenance().manifestFingerprint(),
                    result.provenance().datasetFingerprint(),
                    result.provenance().strategyFingerprint(),
                    result.fingerprint(),
                    manifest == null ? null : manifest.manifestVersion(),
                    manifest == null ? null : DatasetEvidenceResponse.from(manifest.datasetProvenance()),
                    manifest == null ? null : StrategyEvidenceResponse.from(manifest.strategyProvenance()),
                    candidate == null ? null : CandidateEvidenceResponse.from(candidate),
                    manifest == null ? null : manifest.softwareVersion(),
                    manifest == null ? null : manifest.gitCommit());
        }
    }

    public record DatasetEvidenceResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) DatasetVersionId datasetVersionId,
            String version,
            String checksum,
            String provider,
            String tradingPair,
            String timeframe,
            String normalizationVersion,
            Instant rangeStart,
            Instant rangeEnd,
            long candleCount) {
        static DatasetEvidenceResponse from(DatasetProvenanceSnapshot dataset) {
            return new DatasetEvidenceResponse(dataset.datasetVersionId(), dataset.version(), dataset.checksum(),
                    dataset.provider(), dataset.tradingPair(), dataset.timeframe(), dataset.normalizationVersion(),
                    dataset.rangeStart(), dataset.rangeEnd(), dataset.candleCount());
        }
    }

    public record ParameterEvidenceResponse(String type, String value) {}

    public record StrategyReferenceEvidenceResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) StrategyVersionId strategyVersionId,
            @JsonSerialize(using = ToStringSerializer.class) StrategyPluginId pluginId,
            String implementationVersion) {
        static StrategyReferenceEvidenceResponse from(StrategyReference reference) {
            return new StrategyReferenceEvidenceResponse(reference.strategyVersionId(),
                    reference.pluginId(), reference.implementationVersion().toString());
        }
    }

    public record StrategyComponentEvidenceResponse(
            StrategyReferenceEvidenceResponse strategy, Map<String, ParameterEvidenceResponse> parameters) {
        static StrategyComponentEvidenceResponse from(StrategyComponentSnapshot component) {
            return new StrategyComponentEvidenceResponse(
                    StrategyReferenceEvidenceResponse.from(component.strategyReference()),
                    ResultDtos.parameters(component.parameters()));
        }
    }

    public record StrategyEvidenceResponse(
            String kind,
            StrategyReferenceEvidenceResponse singleStrategy,
            Map<String, ParameterEvidenceResponse> parameters,
            @JsonSerialize(using = ToStringSerializer.class) CombinationPolicyId compositePolicyId,
            String compositePolicyVersion,
            List<StrategyComponentEvidenceResponse> components,
            @JsonSerialize(using = TypedUlidSerializer.class) UserStrategyVersionId sourceUserStrategyVersionId,
            String fingerprint) {
        static StrategyEvidenceResponse from(StrategyProvenanceSnapshot strategy) {
            return new StrategyEvidenceResponse(
                    strategy.kind().name(),
                    strategy.singleStrategy().map(StrategyReferenceEvidenceResponse::from).orElse(null),
                    ResultDtos.parameters(strategy.parameters()),
                    strategy.compositePolicyId().orElse(null),
                    strategy.compositePolicyVersion().map(Object::toString).orElse(null),
                    strategy.components().stream().map(StrategyComponentEvidenceResponse::from).toList(),
                    strategy.sourceUserStrategyVersionId().orElse(null),
                    strategy.strategyFingerprint());
        }
    }

    public record CandidateEvidenceResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) CandidateId candidateId,
            int generationIndex,
            Map<String, Object> definition,
            String fingerprint,
            Instant createdAt) {
        static CandidateEvidenceResponse from(CandidateDefinition candidate) {
            return new CandidateEvidenceResponse(candidate.candidateId(), candidate.generationIndex(),
                    candidate.definition(), candidate.fingerprint(), candidate.createdAt());
        }
    }

    private static Map<String, Object> assumptions(BacktestResult result) {
        var assumptions = result.assumptions();
        return Map.of(
                "assumptionsVersion", assumptions.contractVersion(),
                "initialCapital", assumptions.initialCapital().value().toPlainString(),
                "feeRate", assumptions.feeRate().toPlainString(),
                "slippageRate", assumptions.slippageRate().toPlainString(),
                "positionMode", assumptions.positionMode().name(),
                "executionPriceRule", assumptions.executionPriceRule().name(),
                "forceCloseAtEnd", assumptions.forceCloseAtEnd(),
                "roundingMode", assumptions.roundingMode().name());
    }

    private static Map<String, ParameterEvidenceResponse> parameters(StrategyParameterSet parameters) {
        return parameters.values().entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> new ParameterEvidenceResponse(
                        entry.getValue().type().name(), entry.getValue().canonicalText())));
    }
}

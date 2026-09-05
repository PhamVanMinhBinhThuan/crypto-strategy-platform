package com.cryptostrategy.platform.execution.api.port.in;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Application boundary chuyển public input trung tính thành durable Start graph. */
public interface SearchStartCommandFactory {
    StartSearchExperimentUseCase.StartCommand create(Request request);

    record Request(UUID ownerUserId, String idempotencyKey, String canonicalRequestHash,
            String correlationId, String name, DatasetVersionId datasetId, RequestedGeneratorId generatorId,
            String generatorVersion, Long seed, UserStrategyVersionId userStrategyVersionId,
            StrategyPluginId strategyId, String strategyVersion,
            Map<String, ParameterDomain> parameters, Integer maximumCandidates,
            Integer maximumDurationSeconds, Integer maximumWithoutImprovement, Integer topK,
            List<StrategyPoolEntryRequest> strategyPool, Integer minimumComponents,
            Integer maximumComponents, CombinationPolicyId combinationPolicyId,
            String combinationPolicyVersion, List<ComponentConstraintRequest> constraints,
            Integer requestedConcurrency, BacktestAssumptionsRequest backtestAssumptions) {
        public Request(UUID ownerUserId, String idempotencyKey, String canonicalRequestHash,
                String correlationId, String name, DatasetVersionId datasetId,
                RequestedGeneratorId generatorId, String generatorVersion, Long seed,
                UserStrategyVersionId userStrategyVersionId, StrategyPluginId strategyId,
                String strategyVersion, Map<String, ParameterDomain> parameters,
                Integer maximumCandidates, Integer maximumDurationSeconds,
                Integer maximumWithoutImprovement, Integer topK,
                List<StrategyPoolEntryRequest> strategyPool, Integer minimumComponents,
                Integer maximumComponents, CombinationPolicyId combinationPolicyId,
                String combinationPolicyVersion, List<ComponentConstraintRequest> constraints,
                Integer requestedConcurrency) {
            this(ownerUserId, idempotencyKey, canonicalRequestHash, correlationId, name, datasetId,
                    generatorId, generatorVersion, seed, userStrategyVersionId, strategyId,
                    strategyVersion, parameters, maximumCandidates, maximumDurationSeconds,
                    maximumWithoutImprovement, topK, strategyPool, minimumComponents,
                    maximumComponents, combinationPolicyId, combinationPolicyVersion,
                    constraints, requestedConcurrency, null);
        }

        public Request(UUID ownerUserId, String idempotencyKey, String canonicalRequestHash,
                String correlationId, String name, DatasetVersionId datasetId,
                RequestedGeneratorId generatorId, String generatorVersion, Long seed,
                UserStrategyVersionId userStrategyVersionId, StrategyPluginId strategyId,
                String strategyVersion, Map<String, ParameterDomain> parameters,
                Integer maximumCandidates, Integer maximumDurationSeconds, Integer topK) {
            this(ownerUserId, idempotencyKey, canonicalRequestHash, correlationId, name, datasetId,
                    generatorId, generatorVersion, seed, userStrategyVersionId, strategyId,
                    strategyVersion, parameters, maximumCandidates, maximumDurationSeconds, null, topK,
                    List.of(), null, null, null, null, List.of(), null, null);
        }
    }

    record BacktestAssumptionsRequest(BigDecimal initialCapital, BigDecimal feeRate,
            BigDecimal slippageRate) {}

    record StrategyPoolEntryRequest(StrategyPluginId strategyId, String strategyVersion,
            UserStrategyVersionId userStrategyVersionId, Map<String, ParameterDomain> parameters) {}

    record ComponentConstraintRequest(String kind, String left, String right) {}

    record ParameterDomain(String kind, BigDecimal minimum, BigDecimal maximum,
            BigDecimal step, List<String> options) {
        public ParameterDomain(BigDecimal minimum, BigDecimal maximum,
                BigDecimal step, List<String> options) {
            this(null, minimum, maximum, step, options);
        }

        public ParameterDomain(Long minimum, Long maximum, List<String> options) {
            this(null, decimal(minimum), decimal(maximum), null, options);
        }

        private static BigDecimal decimal(Long value) {
            return value == null ? null : BigDecimal.valueOf(value);
        }
    }
}

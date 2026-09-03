package com.cryptostrategy.platform.execution.api.port.in;

import com.cryptostrategy.platform.search.api.model.GeneratorId;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Application boundary chuyển public input trung tính thành durable Start graph. */
public interface SearchStartCommandFactory {
    StartSearchExperimentUseCase.StartCommand create(Request request);

    record Request(UUID ownerUserId, String idempotencyKey, String canonicalRequestHash,
            String correlationId, String name, DatasetVersionId datasetId, String generatorRef,
            String generatorVersion, Long seed, StrategyPluginId strategyId, String strategyVersion,
            Map<String, ParameterDomain> parameters, Integer maximumCandidates,
            Integer maximumDurationSeconds, Integer topK) {}

    record ParameterDomain(Long minimum, Long maximum, List<String> options) {}
}

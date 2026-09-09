package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.api.error.RequestFieldValidationException;
import com.cryptostrategy.platform.execution.api.port.in.SearchStartCommandFactory;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchExperimentUseCase.StartCommand;
import com.cryptostrategy.platform.execution.api.port.in.RequestedGeneratorId;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Chỉ ánh xạ HTTP DTO; validation và domain construction thuộc application boundary. */
@Component
final class ExperimentRequestMapper {
    private final SearchStartCommandFactory commands;

    ExperimentRequestMapper(SearchStartCommandFactory commands) {
        this.commands = Objects.requireNonNull(commands, "commands");
    }

    StartCommand map(UUID owner, String key, String hash, String correlationId,
            CommandDtos.StartExperimentRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.configurationVersion() != null && request.configurationVersion() != 2) {
            throw field("configurationVersion", "Only Search configuration version 2 is supported.");
        }
        if (request.configurationVersion() != null
                && (request.searchSpace() == null || !Integer.valueOf(2).equals(request.searchSpace().schemaVersion()))) {
            throw field("searchSpace.schemaVersion", "Composite Search requires schema version 2.");
        }
        if (request.configurationVersion() != null && request.datasetId() == null) {
            throw field("datasetId", "Select a frozen dataset.");
        }
        if (request.configurationVersion() != null && request.generator() == null) {
            throw field("generator", "Select a Search generator.");
        }
        if (request.generator() != null && request.generator().generatorId() == null) {
            throw field("generator.generatorId", "Select a Search generator.");
        }
        if (request.configurationVersion() != null
                && (request.searchSpace().strategyPool() == null
                        || request.searchSpace().strategyPool().isEmpty())) {
            throw field("searchSpace.strategyPool", "Select at least one Strategy.");
        }
        if (request.configurationVersion() != null
                && request.searchSpace().combinationPolicy() == null) {
            throw field("searchSpace.combinationPolicy", "Select a combination policy.");
        }
        if (request.configurationVersion() != null
                && request.searchSpace().combinationPolicy().policyId() == null) {
            throw field("searchSpace.combinationPolicy.policyId", "Select a combination policy.");
        }
        Map<String, SearchStartCommandFactory.ParameterDomain> parameters = new TreeMap<>();
        if (request.searchSpace() != null && request.searchSpace().parameters() != null) {
            request.searchSpace().parameters().forEach((name, value) -> parameters.put(name,
                    new SearchStartCommandFactory.ParameterDomain(
                            value.kind(), value.min(), value.max(), value.step(), value.values())));
        }
        java.util.List<SearchStartCommandFactory.StrategyPoolEntryRequest> pool =
                request.searchSpace() == null || request.searchSpace().strategyPool() == null
                        ? java.util.List.of()
                        : request.searchSpace().strategyPool().stream().map(entry -> {
                            Map<String, SearchStartCommandFactory.ParameterDomain> domains = new TreeMap<>();
                            if (entry.parameterDomains() != null) {
                                entry.parameterDomains().forEach((name, value) -> domains.put(name,
                                        new SearchStartCommandFactory.ParameterDomain(
                                                value.kind(), value.min(), value.max(), value.step(), value.values())));
                            }
                            return new SearchStartCommandFactory.StrategyPoolEntryRequest(
                                    entry.strategyId(), entry.version(),
                                    entry.userStrategyVersionId(), Map.copyOf(domains));
                        }).toList();
        CommandDtos.StopConditionRequest stops = request.stopConditions() != null
                ? request.stopConditions() : request.stopCondition();
        return commands.create(new SearchStartCommandFactory.Request(
                owner, key, hash, correlationId, request.name(), request.datasetId(),
                request.generator() == null ? null : new RequestedGeneratorId(request.generator().generatorId().value()),
                request.generator() == null ? null : request.generator().version(),
                request.generator() == null ? null : request.generator().seed(),
                request.userStrategyVersionId(),
                request.searchSpace() == null ? null : request.searchSpace().strategyId(),
                request.searchSpace() == null ? null : request.searchSpace().strategyVersion(),
                Map.copyOf(parameters),
                stops == null ? null : stops.maximumCandidates(),
                stops == null ? null : stops.maximumDurationSeconds(),
                stops == null ? null : stops.maximumWithoutImprovement(),
                request.topK(), pool,
                request.searchSpace() == null ? null : request.searchSpace().minComponents(),
                request.searchSpace() == null ? null : request.searchSpace().maxComponents(),
                request.searchSpace() == null || request.searchSpace().combinationPolicy() == null
                        ? null : new com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId(
                                request.searchSpace().combinationPolicy().policyId().value()),
                request.searchSpace() == null || request.searchSpace().combinationPolicy() == null
                        ? null : request.searchSpace().combinationPolicy().version(),
                request.searchSpace() == null || request.searchSpace().constraints() == null
                        ? java.util.List.of()
                        : request.searchSpace().constraints().stream()
                                .map(value -> new SearchStartCommandFactory.ComponentConstraintRequest(
                                        value.kind(), value.left(), value.right()))
                                .toList(),
                request.requestedConcurrency(), backtestAssumptions(request.backtestConfiguration())));
    }

    private static SearchStartCommandFactory.BacktestAssumptionsRequest backtestAssumptions(
            CommandDtos.SearchBacktestConfigurationRequest request) {
        if (request == null) return null;
        var initialCapital = decimal(
                request.initialCapital(), "backtestConfiguration.initialCapital");
        var feeRate = decimal(request.feeRate(), "backtestConfiguration.feeRate");
        var slippageRate = decimal(
                request.slippageRate(), "backtestConfiguration.slippageRate");
        if (initialCapital.signum() <= 0) {
            throw field("backtestConfiguration.initialCapital", "Enter a positive amount.");
        }
        requireRate(feeRate, "backtestConfiguration.feeRate");
        requireRate(slippageRate, "backtestConfiguration.slippageRate");
        return new SearchStartCommandFactory.BacktestAssumptionsRequest(
                initialCapital, feeRate, slippageRate);
    }

    private static void requireRate(java.math.BigDecimal value, String field) {
        if (value.signum() < 0 || value.compareTo(java.math.BigDecimal.ONE) >= 0) {
            throw field(field, "Enter a rate from 0 (inclusive) to 1 (exclusive).");
        }
    }

    private static java.math.BigDecimal decimal(String value, String field) {
        if (value == null || value.isBlank()) {
            throw field(field, "A decimal value is required.");
        }
        try {
            return new java.math.BigDecimal(value);
        } catch (NumberFormatException failure) {
            throw field(field, "Enter an exact decimal value.");
        }
    }

    private static RequestFieldValidationException field(String field, String reason) {
        return new RequestFieldValidationException(field, reason);
    }
}

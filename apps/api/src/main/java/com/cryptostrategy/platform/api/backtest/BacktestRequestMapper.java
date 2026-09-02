package com.cryptostrategy.platform.api.backtest;

import com.cryptostrategy.platform.api.experiment.CommandDtos;
import com.cryptostrategy.platform.backtesting.api.BacktestConfigurationParser;
import com.cryptostrategy.platform.backtesting.api.error.BacktestErrorCode;
import com.cryptostrategy.platform.backtesting.api.error.BacktestException;
import com.cryptostrategy.platform.experiment.api.backtest.StartStandaloneBacktestCommand;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.marketdata.api.port.in.GetDatasetUseCase;
import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterDefinition;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyFingerprintCalculator;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Maps transport input to exact frozen F-003/F-004/F-006 contracts. */
@Component
final class BacktestRequestMapper {
    private final GetDatasetUseCase datasets;
    private final StrategyRegistry strategies;
    private final StrategyFingerprintCalculator fingerprints;
    private final BacktestConfigurationParser configurations;
    private final String softwareVersion;
    private final String gitCommit;

    BacktestRequestMapper(
            GetDatasetUseCase datasets,
            StrategyRegistry strategies,
            StrategyFingerprintCalculator fingerprints,
            BacktestConfigurationParser configurations,
            @Value("${platform.build.version:development}") String softwareVersion,
            @Value("${platform.build.git-commit:unknown}") String gitCommit) {
        this.datasets = Objects.requireNonNull(datasets, "datasets");
        this.strategies = Objects.requireNonNull(strategies, "strategies");
        this.fingerprints = Objects.requireNonNull(fingerprints, "fingerprints");
        this.configurations = Objects.requireNonNull(configurations, "configurations");
        this.softwareVersion = requireText(softwareVersion, "softwareVersion");
        this.gitCommit = requireText(gitCommit, "gitCommit");
    }

    StartStandaloneBacktestCommand map(
            CommandDtos.StartBacktestRequest request,
            String idempotencyKey,
            String requestHash,
            String correlationId) {
        Objects.requireNonNull(request, "request");
        if (request.datasetId() == null
                || request.strategy() == null
                || request.configuration() == null) {
            throw invalid("Dataset, Strategy, and Backtest configuration are required");
        }

        var dataset = datasets.getDataset(new com.cryptostrategy.platform.domain.api.market.DatasetVersionId(request.datasetId()));
        DatasetProvenanceSnapshot datasetProvenance = new DatasetProvenanceSnapshot(
                dataset.datasetVersionId(),
                dataset.version(),
                dataset.checksum(),
                dataset.provider().value(),
                dataset.tradingPair().canonicalSymbol(),
                dataset.timeframe().code(),
                dataset.normalizationVersion(),
                dataset.rangeStart(),
                dataset.rangeEnd(),
                dataset.candleCount());

        var selection = request.strategy();
        if (selection.strategyPlugin() == null || selection.version() == null) {
            throw invalid("Strategy selection is required");
        }
        StrategyPluginId pluginId = new StrategyPluginId(selection.strategyPlugin());
        SemanticVersion version;
        try {
            version = SemanticVersion.parse(selection.version());
        } catch (RuntimeException exception) {
            throw invalid("Strategy version is invalid");
        }
        var descriptor = strategies.descriptor(pluginId, version);
        Map<String, ParameterDefinition> definitions = new HashMap<>();
        descriptor.parameterSchema().definitions()
                .forEach(definition -> definitions.put(definition.name(), definition));
        Map<String, StrategyParameterValue> supplied = new HashMap<>();
        selection.parameters().forEach((name, value) -> {
            ParameterDefinition definition = definitions.get(name);
            if (definition == null) {
                throw invalid("Strategy parameter is unknown");
            }
            supplied.put(name, parameter(definition.type(), value));
        });
        var resolvedParameters = strategies.resolveParameters(pluginId, version, supplied);
        String strategyFingerprint = fingerprints.single(
                descriptor.reference(), resolvedParameters);
        StrategyProvenanceSnapshot strategyProvenance = StrategyProvenanceSnapshot.single(
                descriptor.reference(),
                resolvedParameters,
                java.util.Optional.empty(),
                strategyFingerprint);

        Map<String, Object> backtestConfig = configuration(request.configuration());
        try {
            configurations.parse(backtestConfig);
        } catch (IllegalArgumentException exception) {
            throw new BacktestException(
                    BacktestErrorCode.INVALID_ASSUMPTIONS,
                    "Backtest assumptions are invalid");
        }
        return new StartStandaloneBacktestCommand(
                idempotencyKey,
                requestHash,
                datasetProvenance,
                strategyProvenance,
                backtestConfig,
                Map.of("metricVersion", "metric-v1", "rankingVersion", "ranking-v1"),
                softwareVersion,
                gitCommit,
                correlationId);
    }

    private static Map<String, Object> configuration(
            CommandDtos.BacktestConfigurationRequest request) {
        Map<String, Object> frozen = new LinkedHashMap<>();
        frozen.put("assumptionsVersion", "backtest-assumptions-v1");
        frozen.put("initialCapital", decimal(request.initialCapital(), "initialCapital", true));
        frozen.put("feeRate", decimal(request.feeRate(), "feeRate", false));
        frozen.put("slippageRate", decimal(request.slippageRate(), "slippageRate", false));
        frozen.put("executionPriceRule", requireText(
                request.executionPriceRule(), "executionPriceRule"));
        frozen.put("positionMode", requireText(request.positionMode(), "positionMode"));
        frozen.put("forceCloseAtEnd", request.forceCloseAtEnd());
        frozen.put("roundingMode", requireText(request.roundingMode(), "roundingMode"));
        return Map.copyOf(frozen);
    }

    private static StrategyParameterValue parameter(ParameterType type, JsonNode value) {
        if (value == null || value.isNull()) {
            throw invalid("Strategy parameter cannot be null");
        }
        try {
            return switch (type) {
                case INTEGER -> {
                    if (!value.isIntegralNumber() || !value.canConvertToLong()) {
                        throw invalid("Integer Strategy parameter is invalid");
                    }
                    yield new StrategyParameterValue.IntegerValue(value.longValue());
                }
                case DECIMAL -> new StrategyParameterValue.DecimalValue(decimalNode(value));
                case BOOLEAN -> {
                    if (!value.isBoolean()) {
                        throw invalid("Boolean Strategy parameter is invalid");
                    }
                    yield new StrategyParameterValue.BooleanValue(value.booleanValue());
                }
                case TEXT -> new StrategyParameterValue.TextValue(textNode(value));
                case ENUM -> new StrategyParameterValue.EnumValue(textNode(value));
            };
        } catch (NumberFormatException exception) {
            throw invalid("Decimal Strategy parameter is invalid");
        }
    }

    private static String decimal(String raw, String name, boolean positive) {
        try {
            BigDecimal value = new BigDecimal(requireText(raw, name));
            if ((positive && value.signum() <= 0) || (!positive && value.signum() < 0)) {
                throw invalid(name + " is outside its supported range");
            }
            return value.signum() == 0
                    ? BigDecimal.ZERO.toPlainString()
                    : value.stripTrailingZeros().toPlainString();
        } catch (NumberFormatException exception) {
            throw invalid(name + " must be an exact decimal string");
        }
    }

    private static BigDecimal decimalNode(JsonNode value) {
        if (value.isTextual()) {
            return new BigDecimal(value.textValue());
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        throw invalid("Decimal Strategy parameter is invalid");
    }

    private static String textNode(JsonNode value) {
        if (!value.isTextual()) {
            throw invalid("Text Strategy parameter is invalid");
        }
        return value.textValue();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw invalid(name + " is required");
        }
        return value;
    }

    private static StrategyException invalid(String message) {
        return new StrategyException(StrategyErrorCode.INVALID_PARAMETERS, message);
    }
}

package com.cryptostrategy.platform.api.strategy;

import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyKind;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterDefinition;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import com.cryptostrategy.platform.strategy.api.model.user.CompositeStrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.SingleStrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.StrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.UserStrategyComponent;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
final class StrategyRequestMapper {
    private final StrategyRegistry registry;

    StrategyRequestMapper(StrategyRegistry registry) {
        this.registry = registry;
    }

    StrategyKind kind(String value) {
        try {
            return StrategyKind.valueOf(Objects.requireNonNull(value, "kind"));
        } catch (RuntimeException exception) {
            throw invalid("Strategy kind is invalid");
        }
    }

    StrategyDraftSource source(
            StrategyKind expectedKind, StrategyDtos.StrategySourceRequest request) {
        if (request == null || request.type() == null) {
            throw invalid("Strategy source is required");
        }
        StrategyKind sourceKind = kind(request.type());
        if (sourceKind != expectedKind) {
            throw invalid("Strategy source type does not match kind");
        }
        if (sourceKind == StrategyKind.SINGLE) {
            if (request.strategy() == null
                    || request.policy() != null
                    || request.policyVersion() != null
                    || !request.policyParameters().isEmpty()
                    || !request.components().isEmpty()) {
                throw invalid("Single Strategy source is malformed");
            }
            var selection = selection(request.strategy());
            return new SingleStrategyDraftSource(
                    selection.reference(), selection.parameters());
        }
        if (request.strategy() != null
                || request.policy() == null
                || request.policyVersion() == null
                || request.components().size() < 2) {
            throw invalid("Composite Strategy source is malformed");
        }
        return new CompositeStrategyDraftSource(
                new CombinationPolicyId(request.policy()),
                SemanticVersion.parse(request.policyVersion()),
                inferParameters(request.policyParameters()),
                request.components().stream()
                        .map(this::selection)
                        .map(value -> new UserStrategyComponent(
                                value.reference(), value.parameters()))
                        .toList());
    }

    private ResolvedSelection selection(
            StrategyDtos.StrategySelectionRequest selection) {
        if (selection == null
                || selection.strategyPlugin() == null
                || selection.version() == null) {
            throw invalid("Strategy selection is required");
        }
        try {
            var descriptor = registry.descriptor(
                    new com.cryptostrategy.platform.strategy.api.model.StrategyPluginId(
                            selection.strategyPlugin()),
                    SemanticVersion.parse(selection.version()));
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
            return new ResolvedSelection(
                    descriptor.reference(), StrategyParameterSet.of(supplied));
        } catch (StrategyException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalid("Strategy selection is invalid");
        }
    }

    private static StrategyParameterSet inferParameters(Map<String, JsonNode> values) {
        Map<String, StrategyParameterValue> parameters = new HashMap<>();
        values.forEach((name, value) -> parameters.put(name, inferParameter(value)));
        return StrategyParameterSet.of(parameters);
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
                case DECIMAL -> new StrategyParameterValue.DecimalValue(decimal(value));
                case BOOLEAN -> {
                    if (!value.isBoolean()) {
                        throw invalid("Boolean Strategy parameter is invalid");
                    }
                    yield new StrategyParameterValue.BooleanValue(value.booleanValue());
                }
                case TEXT -> new StrategyParameterValue.TextValue(text(value));
                case ENUM -> new StrategyParameterValue.EnumValue(text(value));
            };
        } catch (NumberFormatException exception) {
            throw invalid("Decimal Strategy parameter is invalid");
        }
    }

    private static StrategyParameterValue inferParameter(JsonNode value) {
        if (value == null || value.isNull()) {
            throw invalid("Strategy parameter cannot be null");
        }
        if (value.isIntegralNumber() && value.canConvertToLong()) {
            return new StrategyParameterValue.IntegerValue(value.longValue());
        }
        if (value.isNumber()) {
            return new StrategyParameterValue.DecimalValue(value.decimalValue());
        }
        if (value.isBoolean()) {
            return new StrategyParameterValue.BooleanValue(value.booleanValue());
        }
        if (value.isTextual()) {
            return new StrategyParameterValue.TextValue(value.textValue());
        }
        throw invalid("Strategy parameter type is unsupported");
    }

    private static BigDecimal decimal(JsonNode value) {
        if (value.isTextual()) {
            return new BigDecimal(value.textValue());
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        throw invalid("Decimal Strategy parameter is invalid");
    }

    private static String text(JsonNode value) {
        if (!value.isTextual()) {
            throw invalid("Text Strategy parameter is invalid");
        }
        return value.textValue();
    }

    private static StrategyException invalid(String message) {
        return new StrategyException(StrategyErrorCode.INVALID_PARAMETERS, message);
    }

    private record ResolvedSelection(
            com.cryptostrategy.platform.strategy.api.model.StrategyReference reference,
            StrategyParameterSet parameters) {}
}

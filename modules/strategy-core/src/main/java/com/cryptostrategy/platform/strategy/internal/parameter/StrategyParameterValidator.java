package com.cryptostrategy.platform.strategy.internal.parameter;

import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.parameter.CrossParameterConstraint;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterDefinition;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSchema;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public final class StrategyParameterValidator {
    public StrategyParameterSet resolve(StrategyParameterSchema schema, Map<String, StrategyParameterValue> supplied) {
        Map<String, ParameterDefinition> definitions = new HashMap<>();
        schema.definitions().forEach(definition -> definitions.put(definition.name(), definition));
        for (String name : supplied.keySet()) if (!definitions.containsKey(name)) fail("Unknown parameter: " + name);
        Map<String, StrategyParameterValue> resolved = new HashMap<>();
        for (ParameterDefinition definition : schema.definitions()) {
            StrategyParameterValue value = supplied.get(definition.name());
            if (value == null) value = definition.defaultValue().orElse(null);
            if (value == null && definition.required()) fail("Missing parameter: " + definition.name());
            if (value == null) continue;
            if (value.type() != definition.type()) fail("Wrong type: " + definition.name());
            validateValue(definition, value); resolved.put(definition.name(), value);
        }
        StrategyParameterSet result = StrategyParameterSet.of(resolved);
        for (CrossParameterConstraint constraint : schema.constraints()) {
            BigDecimal lower = numeric(result.require(constraint.lowerParameter()));
            BigDecimal upper = numeric(result.require(constraint.upperParameter()));
            if (lower.compareTo(upper) >= 0) fail(constraint.lowerParameter() + " must be less than " + constraint.upperParameter());
        }
        return result;
    }
    private static void validateValue(ParameterDefinition definition, StrategyParameterValue value) {
        if (definition.type() == ParameterType.ENUM && !definition.allowedValues().contains(value.canonicalText())) fail("Value not allowed: " + definition.name());
        if (definition.type() == ParameterType.INTEGER || definition.type() == ParameterType.DECIMAL) {
            BigDecimal number = numeric(value);
            if (definition.minimum().isPresent() && number.compareTo(definition.minimum().get()) < 0) fail("Below minimum: " + definition.name());
            if (definition.maximum().isPresent() && number.compareTo(definition.maximum().get()) > 0) fail("Above maximum: " + definition.name());
        }
    }
    private static BigDecimal numeric(StrategyParameterValue value) {
        if (value instanceof StrategyParameterValue.IntegerValue integer) return BigDecimal.valueOf(integer.value());
        if (value instanceof StrategyParameterValue.DecimalValue decimal) return decimal.value();
        fail("Expected numeric parameter"); return BigDecimal.ZERO;
    }
    private static void fail(String message) { throw new StrategyException(StrategyErrorCode.INVALID_PARAMETERS, message); }
}

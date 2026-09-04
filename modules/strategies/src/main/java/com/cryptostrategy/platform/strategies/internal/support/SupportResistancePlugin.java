package com.cryptostrategy.platform.strategies.internal.support;

import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.StrategyPlugin;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterDefinition;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSchema;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SupportResistancePlugin implements StrategyPlugin {
    public static final StrategyPluginId PLUGIN_ID = new StrategyPluginId("support-resistance");
    public static final SemanticVersion VERSION = new SemanticVersion(1, 0, 0);

    private static final String BOUNCE = "BOUNCE";
    private static final StrategyReference REFERENCE = new StrategyReference(
            new StrategyVersionId("01J00000000000000000000030"), PLUGIN_ID, VERSION);
    private static final StrategyDescriptor DESCRIPTOR = new StrategyDescriptor(
            REFERENCE,
            "strategy-contract-v1",
            "Support / Resistance",
            "Uses recent price extremes as support and resistance bounce zones",
            "STRUCTURE",
            Set.of(StrategySignal.BUY, StrategySignal.SELL, StrategySignal.HOLD),
            21,
            new StrategyParameterSchema(
                    List.of(
                            integer("lookback", 20, 2, 500),
                            decimal("tolerancePercent", "1", "0", "25"),
                            enumeration("ruleMode", BOUNCE, Set.of(BOUNCE))),
                    List.of()),
            "strategy-descriptor-v1:support-resistance:1.0.0");

    @Override
    public StrategyDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public Strategy create(StrategyParameterSet parameters) {
        long lookback = ((StrategyParameterValue.IntegerValue) parameters.require("lookback")).value();
        BigDecimal tolerancePercent = ((StrategyParameterValue.DecimalValue)
                        parameters.require("tolerancePercent"))
                .value();
        String ruleMode =
                ((StrategyParameterValue.EnumValue) parameters.require("ruleMode")).value();
        return new SupportResistanceStrategy(
                REFERENCE, Math.toIntExact(lookback), tolerancePercent, ruleMode);
    }

    @Override
    public int requiredLookback(StrategyParameterSet parameters) {
        long lookback = ((StrategyParameterValue.IntegerValue) parameters.require("lookback")).value();
        return Math.addExact(Math.toIntExact(lookback), 1);
    }

    private static ParameterDefinition integer(
            String name, long defaultValue, long minimum, long maximum) {
        return new ParameterDefinition(
                name,
                ParameterType.INTEGER,
                true,
                Optional.of(new StrategyParameterValue.IntegerValue(defaultValue)),
                Optional.of(BigDecimal.valueOf(minimum)),
                Optional.of(BigDecimal.valueOf(maximum)),
                Set.of(),
                name);
    }

    private static ParameterDefinition decimal(
            String name, String defaultValue, String minimum, String maximum) {
        return new ParameterDefinition(
                name,
                ParameterType.DECIMAL,
                true,
                Optional.of(new StrategyParameterValue.DecimalValue(new BigDecimal(defaultValue))),
                Optional.of(new BigDecimal(minimum)),
                Optional.of(new BigDecimal(maximum)),
                Set.of(),
                name);
    }

    private static ParameterDefinition enumeration(
            String name, String defaultValue, Set<String> allowedValues) {
        return new ParameterDefinition(
                name,
                ParameterType.ENUM,
                true,
                Optional.of(new StrategyParameterValue.EnumValue(defaultValue)),
                Optional.empty(),
                Optional.empty(),
                allowedValues,
                name);
    }
}

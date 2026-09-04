package com.cryptostrategy.platform.strategies.internal.rsi;

import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.StrategyPlugin;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.CrossParameterConstraint;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterDefinition;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSchema;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class RsiPlugin implements StrategyPlugin {
    public static final StrategyPluginId PLUGIN_ID = new StrategyPluginId("rsi-threshold");
    public static final SemanticVersion VERSION = new SemanticVersion(1, 0, 0);

    private static final StrategyReference REFERENCE = new StrategyReference(
            new StrategyVersionId("01J00000000000000000000010"), PLUGIN_ID, VERSION);
    private static final StrategyDescriptor DESCRIPTOR = new StrategyDescriptor(
            REFERENCE,
            "strategy-contract-v1",
            "RSI Threshold",
            "Uses Relative Strength Index thresholds to identify oversold and overbought conditions",
            "MOMENTUM",
            Set.of(StrategySignal.BUY, StrategySignal.SELL, StrategySignal.HOLD),
            15,
            new StrategyParameterSchema(
                    List.of(
                            integer("period", 14, 2, 500),
                            decimal("buyThreshold", "30", "0", "100"),
                            decimal("sellThreshold", "70", "0", "100")),
                    List.of(new CrossParameterConstraint("buyThreshold", "sellThreshold"))),
            "strategy-descriptor-v1:rsi-threshold:1.0.0");

    @Override
    public StrategyDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public Strategy create(StrategyParameterSet parameters) {
        long period = ((StrategyParameterValue.IntegerValue) parameters.require("period")).value();
        BigDecimal buyThreshold =
                ((StrategyParameterValue.DecimalValue) parameters.require("buyThreshold")).value();
        BigDecimal sellThreshold =
                ((StrategyParameterValue.DecimalValue) parameters.require("sellThreshold")).value();
        return new RsiStrategy(
                REFERENCE, Math.toIntExact(period), buyThreshold, sellThreshold);
    }

    @Override
    public int requiredLookback(StrategyParameterSet parameters) {
        long period = ((StrategyParameterValue.IntegerValue) parameters.require("period")).value();
        return Math.addExact(Math.toIntExact(period), 1);
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
}

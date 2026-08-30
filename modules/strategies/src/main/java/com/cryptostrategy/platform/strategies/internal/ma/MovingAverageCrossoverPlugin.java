package com.cryptostrategy.platform.strategies.internal.ma;

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

public final class MovingAverageCrossoverPlugin implements StrategyPlugin {
    public static final StrategyPluginId PLUGIN_ID = new StrategyPluginId("ma-crossover");
    public static final SemanticVersion VERSION = new SemanticVersion(1, 0, 0);
    private static final StrategyReference REFERENCE = new StrategyReference(
            new StrategyVersionId("01J00000000000000000000000"), PLUGIN_ID, VERSION);
    private static final StrategyDescriptor DESCRIPTOR = new StrategyDescriptor(REFERENCE, "strategy-contract-v1",
            "Moving Average Crossover", "Compares fast and slow simple moving averages", "TREND",
            Set.of(StrategySignal.BUY, StrategySignal.SELL, StrategySignal.HOLD), 25,
            new StrategyParameterSchema(List.of(
                    integer("fastPeriod", 5, 2, 100), integer("slowPeriod", 25, 3, 500)),
                    List.of(new CrossParameterConstraint("fastPeriod", "slowPeriod"))),
            "strategy-descriptor-v1:ma-crossover:1.0.0");
    private static ParameterDefinition integer(String name, long defaultValue, long minimum, long maximum) {
        return new ParameterDefinition(name, ParameterType.INTEGER, true,
                Optional.of(new StrategyParameterValue.IntegerValue(defaultValue)),
                Optional.of(BigDecimal.valueOf(minimum)), Optional.of(BigDecimal.valueOf(maximum)), Set.of(), name);
    }
    @Override public StrategyDescriptor descriptor() { return DESCRIPTOR; }
    @Override public Strategy create(StrategyParameterSet parameters) {
        long fast = ((StrategyParameterValue.IntegerValue) parameters.require("fastPeriod")).value();
        long slow = ((StrategyParameterValue.IntegerValue) parameters.require("slowPeriod")).value();
        return new MovingAverageCrossoverStrategy(REFERENCE, Math.toIntExact(fast), Math.toIntExact(slow));
    }
}

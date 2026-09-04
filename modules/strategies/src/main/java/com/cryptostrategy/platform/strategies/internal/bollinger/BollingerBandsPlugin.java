package com.cryptostrategy.platform.strategies.internal.bollinger;

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

public final class BollingerBandsPlugin implements StrategyPlugin {
    public static final StrategyPluginId PLUGIN_ID = new StrategyPluginId("bollinger-bands");
    public static final SemanticVersion VERSION = new SemanticVersion(1, 0, 0);

    private static final String MEAN_REVERSION = "MEAN_REVERSION";
    private static final StrategyReference REFERENCE = new StrategyReference(
            new StrategyVersionId("01J00000000000000000000020"), PLUGIN_ID, VERSION);
    private static final StrategyDescriptor DESCRIPTOR = new StrategyDescriptor(
            REFERENCE,
            "strategy-contract-v1",
            "Bollinger Bands",
            "Compares the closing price with volatility bands around a moving average",
            "VOLATILITY",
            Set.of(StrategySignal.BUY, StrategySignal.SELL, StrategySignal.HOLD),
            20,
            new StrategyParameterSchema(
                    List.of(
                            integer("period", 20, 2, 500),
                            decimal("standardDeviation", "2", "0.1", "10"),
                            enumeration("ruleMode", MEAN_REVERSION, Set.of(MEAN_REVERSION))),
                    List.of()),
            "strategy-descriptor-v1:bollinger-bands:1.0.0");

    @Override
    public StrategyDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public Strategy create(StrategyParameterSet parameters) {
        long period = ((StrategyParameterValue.IntegerValue) parameters.require("period")).value();
        BigDecimal standardDeviation = ((StrategyParameterValue.DecimalValue)
                        parameters.require("standardDeviation"))
                .value();
        String ruleMode =
                ((StrategyParameterValue.EnumValue) parameters.require("ruleMode")).value();
        return new BollingerBandsStrategy(
                REFERENCE, Math.toIntExact(period), standardDeviation, ruleMode);
    }

    @Override
    public int requiredLookback(StrategyParameterSet parameters) {
        long period = ((StrategyParameterValue.IntegerValue) parameters.require("period")).value();
        return Math.toIntExact(period);
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

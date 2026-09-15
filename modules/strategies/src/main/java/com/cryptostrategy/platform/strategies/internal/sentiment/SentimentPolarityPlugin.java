package com.cryptostrategy.platform.strategies.internal.sentiment;

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
import com.cryptostrategy.platform.strategy.api.model.parameter.SearchRangeHint;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSchema;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SentimentPolarityPlugin implements StrategyPlugin {
    public static final StrategyPluginId PLUGIN_ID = new StrategyPluginId("sentiment-polarity");
    public static final SemanticVersion VERSION = new SemanticVersion(1, 0, 0);
    public static final String INPUT_TYPE = "sentiment-polarity";
    public static final String INPUT_SCHEMA_VERSION = "sentiment-snapshot-v1";

    private static final StrategyReference REFERENCE = new StrategyReference(
            new StrategyVersionId("01J00000000000000000000040"), PLUGIN_ID, VERSION);
    private static final StrategyDescriptor DESCRIPTOR = new StrategyDescriptor(
            REFERENCE,
            "strategy-contract-v1",
            "Sentiment Polarity",
            "Uses frozen News sentiment as a research signal; it is not financial advice",
            "SENTIMENT",
            Set.of(StrategySignal.BUY, StrategySignal.SELL, StrategySignal.HOLD),
            1,
            new StrategyParameterSchema(
                    List.of(
                            integer("lookbackHours", 24, 1, 720, 6, 72, 6,
                                    "Hours of published News considered before each Candle closes."),
                            integer("minimumArticles", 3, 1, 1000, 1, 10, 1,
                                    "Minimum eligible articles required before emitting BUY or SELL."),
                            decimal("buyThreshold", "0.25", "0.0000000001", "1", "0.1", "0.5", "0.05",
                                    "Weighted sentiment score at or above this value emits BUY."),
                            decimal("sellThreshold", "-0.25", "-1", "-0.0000000001", "-0.5", "-0.1", "0.05",
                                    "Weighted sentiment score at or below this value emits SELL.")),
                    List.of(new CrossParameterConstraint("sellThreshold", "buyThreshold"))),
            "strategy-descriptor-v1:sentiment-polarity:1.0.0");

    @Override
    public StrategyDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public Strategy create(StrategyParameterSet parameters) {
        long lookbackHours = ((StrategyParameterValue.IntegerValue)
                parameters.require("lookbackHours")).value();
        long minimumArticles = ((StrategyParameterValue.IntegerValue)
                parameters.require("minimumArticles")).value();
        BigDecimal buyThreshold = ((StrategyParameterValue.DecimalValue)
                parameters.require("buyThreshold")).value();
        BigDecimal sellThreshold = ((StrategyParameterValue.DecimalValue)
                parameters.require("sellThreshold")).value();
        return new SentimentPolarityStrategy(REFERENCE, Math.toIntExact(lookbackHours),
                Math.toIntExact(minimumArticles), buyThreshold, sellThreshold);
    }

    private static ParameterDefinition integer(String name, long defaultValue, long minimum,
            long maximum, long searchMinimum, long searchMaximum, long searchStep,
            String description) {
        return new ParameterDefinition(name, ParameterType.INTEGER, true,
                Optional.of(new StrategyParameterValue.IntegerValue(defaultValue)),
                Optional.of(BigDecimal.valueOf(minimum)), Optional.of(BigDecimal.valueOf(maximum)),
                Set.of(), description, Optional.of(new SearchRangeHint(BigDecimal.valueOf(searchMinimum),
                        BigDecimal.valueOf(searchMaximum), BigDecimal.valueOf(searchStep))));
    }

    private static ParameterDefinition decimal(String name, String defaultValue, String minimum,
            String maximum, String searchMinimum, String searchMaximum, String searchStep,
            String description) {
        return new ParameterDefinition(name, ParameterType.DECIMAL, true,
                Optional.of(new StrategyParameterValue.DecimalValue(new BigDecimal(defaultValue))),
                Optional.of(new BigDecimal(minimum)), Optional.of(new BigDecimal(maximum)), Set.of(),
                description, Optional.of(new SearchRangeHint(new BigDecimal(searchMinimum),
                        new BigDecimal(searchMaximum), new BigDecimal(searchStep))));
    }
}

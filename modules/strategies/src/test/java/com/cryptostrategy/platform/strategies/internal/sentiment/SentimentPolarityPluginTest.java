package com.cryptostrategy.platform.strategies.internal.sentiment;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.strategy.api.model.StrategySignal;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SentimentPolarityPluginTest {
    @Test
    void publishesTheFifthStrategyContract() {
        var descriptor = new SentimentPolarityPlugin().descriptor();

        assertEquals("sentiment-polarity", descriptor.reference().pluginId().value());
        assertEquals("SENTIMENT", descriptor.category());
        assertEquals(4, descriptor.parameterSchema().definitions().size());
        assertEquals(1, descriptor.requiredLookback());
        assertEquals(StrategySignal.values().length, descriptor.supportedSignals().size());
    }

    @Test
    void rejectsInvalidCrossThresholdConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new SentimentPolarityPlugin().create(
                parameters(24, 3, "-0.1", "-0.25")));
    }

    static StrategyParameterSet parameters(long lookbackHours, long minimumArticles,
            String buyThreshold, String sellThreshold) {
        return StrategyParameterSet.of(Map.of(
                "lookbackHours", new StrategyParameterValue.IntegerValue(lookbackHours),
                "minimumArticles", new StrategyParameterValue.IntegerValue(minimumArticles),
                "buyThreshold", new StrategyParameterValue.DecimalValue(new BigDecimal(buyThreshold)),
                "sellThreshold", new StrategyParameterValue.DecimalValue(new BigDecimal(sellThreshold))));
    }
}

package com.cryptostrategy.platform.strategy.api;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.model.*;
import com.cryptostrategy.platform.strategy.api.model.parameter.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class StrategyLookbackContractTest {
    @Test void pluginMayResolveLookbackFromFrozenParameters() {
        StrategyPlugin plugin = new StrategyPlugin() {
            public StrategyDescriptor descriptor() { return null; }
            public Strategy create(StrategyParameterSet p) { return null; }
            public int requiredLookback(StrategyParameterSet p) {
                return Math.toIntExact(((StrategyParameterValue.IntegerValue)p.require("slowPeriod")).value());
            }
        };
        var parameters = StrategyParameterSet.of(Map.of("slowPeriod", new StrategyParameterValue.IntegerValue(50)));
        assertEquals(50, plugin.requiredLookback(parameters));
        assertEquals(Set.of("slowPeriod"), parameters.values().keySet());
    }
}

package com.cryptostrategy.platform.backtesting.api;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BacktestConfigurationParserTest {
    @Test void parsesOnlyCompleteFrozenConfiguration() {
        var parsed = new BacktestConfigurationParser().parse(Map.of(
                "assumptionsVersion", "backtest-assumptions-v1", "initialCapital", "1000.00",
                "feeRate", "0.001", "slippageRate", "0.0005",
                "executionPriceRule", "NEXT_CANDLE_OPEN", "positionMode", "LONG_ONLY",
                "forceCloseAtEnd", true, "roundingMode", "HALF_EVEN"));
        assertEquals("backtest-assumptions-v1", parsed.contractVersion());
        assertEquals(0, parsed.initialCapital().value().compareTo(new java.math.BigDecimal("1000")));
    }

    @Test void rejectsMissingValuesRatherThanApplyingHiddenDefaults() {
        assertThrows(IllegalArgumentException.class,
                () -> new BacktestConfigurationParser().parse(Map.of("initialCapital", "1000")));
    }
}

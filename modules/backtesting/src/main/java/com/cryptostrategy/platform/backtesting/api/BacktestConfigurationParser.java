package com.cryptostrategy.platform.backtesting.api;

import com.cryptostrategy.platform.backtesting.api.model.BacktestAssumptions;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Converts the frozen Manifest map into a complete typed contract; no hidden defaults are used. */
public final class BacktestConfigurationParser {
    private static final Set<String> KEYS = Set.of("assumptionsVersion", "initialCapital", "feeRate",
            "slippageRate", "executionPriceRule", "positionMode", "forceCloseAtEnd", "roundingMode");

    public BacktestAssumptions parse(Map<String, Object> config) {
        Objects.requireNonNull(config, "config");
        if (!config.keySet().equals(KEYS)) {
            throw new IllegalArgumentException("Backtest config must contain exactly: " + KEYS);
        }
        require(config, "assumptionsVersion", "backtest-assumptions-v1");
        require(config, "executionPriceRule", "NEXT_CANDLE_OPEN");
        require(config, "positionMode", "LONG_ONLY");
        require(config, "forceCloseAtEnd", Boolean.TRUE);
        require(config, "roundingMode", "HALF_EVEN");
        return BacktestAssumptions.mvp(decimal(config, "initialCapital"), decimal(config, "feeRate"),
                decimal(config, "slippageRate"));
    }

    private static BigDecimal decimal(Map<String, Object> config, String key) {
        Object value = Objects.requireNonNull(config.get(key), key);
        try { return new BigDecimal(value.toString()); }
        catch (NumberFormatException error) { throw new IllegalArgumentException("Invalid decimal: " + key, error); }
    }

    private static void require(Map<String, Object> config, String key, Object expected) {
        if (!expected.equals(config.get(key))) throw new IllegalArgumentException("Unsupported " + key);
    }
}

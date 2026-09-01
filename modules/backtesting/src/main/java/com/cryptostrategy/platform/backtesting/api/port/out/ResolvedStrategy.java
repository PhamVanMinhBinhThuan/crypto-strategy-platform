package com.cryptostrategy.platform.backtesting.api.port.out;

import com.cryptostrategy.platform.strategy.api.Strategy;
import java.util.Objects;

public record ResolvedStrategy(Strategy strategy, int requiredLookback, String verifiedFingerprint) {
    public ResolvedStrategy {
        Objects.requireNonNull(strategy);
        Objects.requireNonNull(verifiedFingerprint);
        if (requiredLookback < 1) throw new IllegalArgumentException("requiredLookback must be positive");
    }
}

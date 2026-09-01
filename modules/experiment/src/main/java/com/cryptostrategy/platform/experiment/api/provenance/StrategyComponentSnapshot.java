package com.cryptostrategy.platform.experiment.api.provenance;

import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.util.Objects;

public record StrategyComponentSnapshot(
        StrategyReference strategyReference,
        StrategyParameterSet parameters
) {
    public StrategyComponentSnapshot {
        Objects.requireNonNull(strategyReference, "strategyReference cannot be null");
        Objects.requireNonNull(parameters, "parameters cannot be null");
    }
}

package com.cryptostrategy.platform.backtesting.api.port.out;

import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.strategy.api.model.StrategySupplementalInput;
import java.util.Map;

/** Resolves only immutable inputs referenced by the already-frozen Experiment manifest. */
@FunctionalInterface
public interface FrozenSupplementalInputResolver {
    Map<String, StrategySupplementalInput> resolve(ExperimentManifest manifest);

    static FrozenSupplementalInputResolver none() {
        return manifest -> Map.of();
    }
}

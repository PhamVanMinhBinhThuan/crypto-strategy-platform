package com.cryptostrategy.platform.combination.api;

import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.util.List;

/** Public owner boundary for constructing a composite without exposing its implementation. */
public interface CompositeStrategyMaterializer {
    Strategy materialize(StrategyReference reference, CombinationPolicyReference policy,
            StrategyParameterSet policyParameters, List<Strategy> orderedComponents);
}

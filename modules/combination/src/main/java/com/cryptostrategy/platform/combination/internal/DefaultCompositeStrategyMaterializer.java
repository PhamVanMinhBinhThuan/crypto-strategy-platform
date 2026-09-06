package com.cryptostrategy.platform.combination.internal;

import com.cryptostrategy.platform.combination.api.*;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.util.List;
import java.util.Objects;

public final class DefaultCompositeStrategyMaterializer implements CompositeStrategyMaterializer {
    private final CompositeStrategyFactory factory;

    public DefaultCompositeStrategyMaterializer(List<CombinationPolicy> policies) {
        this.factory = new CompositeStrategyFactory(policies);
    }

    @Override
    public Strategy materialize(StrategyReference reference, CombinationPolicyReference policy,
            StrategyParameterSet policyParameters, List<Strategy> orderedComponents) {
        Objects.requireNonNull(orderedComponents, "orderedComponents");
        if (orderedComponents.size() < 2 || orderedComponents.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Composite requires at least two ordered components");
        }
        return factory.create(reference, policy, policyParameters, List.copyOf(orderedComponents));
    }
}

package com.cryptostrategy.platform.combination.api;

import com.cryptostrategy.platform.combination.internal.DefaultCompositeStrategyMaterializer;

import java.util.List;

public final class CombinationModuleFactory {
    private CombinationModuleFactory() {}

    public static CompositeStrategyMaterializer materializer(List<CombinationPolicy> policies) {
        return new DefaultCompositeStrategyMaterializer(policies);
    }
}

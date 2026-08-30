package com.cryptostrategy.platform.strategy.internal.application;
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.cryptostrategy.platform.strategy.api.port.out.StrategyCatalogStore;
import java.util.Objects;
public final class StrategyCatalogSynchronizer {
    private final StrategyRegistry registry; private final StrategyCatalogStore store;
    public StrategyCatalogSynchronizer(StrategyRegistry registry, StrategyCatalogStore store) { this.registry=Objects.requireNonNull(registry); this.store=Objects.requireNonNull(store); }
    public void synchronize() { for (StrategyDescriptor descriptor : registry.listAvailable()) store.registerOrVerify(descriptor); }
}

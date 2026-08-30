package com.cryptostrategy.platform.strategy.api;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyCatalogSynchronization;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.cryptostrategy.platform.strategy.api.port.in.UserStrategyApplication;
import com.cryptostrategy.platform.strategy.api.port.out.StrategyCatalogStore;
import com.cryptostrategy.platform.strategy.api.port.out.UserStrategyStore;
import com.cryptostrategy.platform.strategy.internal.application.StrategyCatalogSynchronizer;
import com.cryptostrategy.platform.strategy.internal.application.UserStrategyService;
import com.cryptostrategy.platform.strategy.internal.registry.DefaultStrategyRegistry;
import java.time.Clock;
import java.util.List;
public final class StrategyModuleFactory {
    private StrategyModuleFactory(){}
    public static StrategyRegistry registry(List<StrategyPlugin> plugins){return new DefaultStrategyRegistry(plugins);}
    public static StrategyCatalogSynchronization catalogSynchronization(StrategyRegistry registry,StrategyCatalogStore store){return new StrategyCatalogSynchronizer(registry,store)::synchronize;}
    public static UserStrategyApplication userStrategies(StrategyRegistry registry,UserStrategyStore store,Clock clock){return new UserStrategyService(registry,store,clock);}
}

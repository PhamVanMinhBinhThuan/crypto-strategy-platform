package com.cryptostrategy.platform.api.config;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategies.api.StrategyPlugins;
import com.cryptostrategy.platform.strategy.internal.registry.DefaultStrategyRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
class StrategyConfigurationTest {
    @Test void trustedPluginCompositionHasNoDeliveryEndpoint(){assertEquals(4,new DefaultStrategyRegistry(StrategyPlugins.trusted()).listAvailable().size());assertFalse(StrategyConfiguration.class.getName().toLowerCase().contains("controller"));}
    @Test void startupSynchronizesTheRuntimeCatalog() throws Exception {var calls=new AtomicInteger();var runner=new StrategyConfiguration().strategyCatalogStartup(calls::incrementAndGet);runner.run(null);assertEquals(1,calls.get());}
}

package com.cryptostrategy.platform.architecture;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.architecture.fixtures.strategyextension.MacdStrategyPluginFixture;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.internal.registry.DefaultStrategyRegistry;
import java.util.*;
import org.junit.jupiter.api.Test;
class StrategyExtensionTest {
    @Test void testOnlyMacdUsesTheExistingExtensionContract(){DefaultStrategyRegistry registry=new DefaultStrategyRegistry(List.of(new MacdStrategyPluginFixture()));assertEquals("macd",registry.listAvailable().getFirst().reference().pluginId().value());assertNotNull(registry.create(new StrategyPluginId("macd"),new SemanticVersion(1,0,0),Map.of()));}
}

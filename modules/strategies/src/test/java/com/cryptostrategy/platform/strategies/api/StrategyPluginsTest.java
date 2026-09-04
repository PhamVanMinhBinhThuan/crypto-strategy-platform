package com.cryptostrategy.platform.strategies.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptostrategy.platform.strategy.api.StrategyPlugin;
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.internal.parameter.StrategyParameterValidator;
import com.cryptostrategy.platform.strategy.internal.registry.DefaultStrategyRegistry;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StrategyPluginsTest {
    private static final Set<String> EXPECTED_PLUGIN_IDS = Set.of(
            "ma-crossover", "rsi-threshold", "bollinger-bands", "support-resistance");

    @Test
    void trustedCatalogPublishesAllFourRequiredStrategies() {
        List<StrategyPlugin> plugins = StrategyPlugins.trusted();
        DefaultStrategyRegistry registry = new DefaultStrategyRegistry(plugins);

        assertEquals(4, plugins.size());
        assertEquals(
                EXPECTED_PLUGIN_IDS,
                registry.listAvailable().stream()
                        .map(descriptor -> descriptor.reference().pluginId().value())
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void everyTrustedPluginHasAUniqueIdAndImplementationVersion() {
        Set<String> identities = new HashSet<>();

        for (StrategyPlugin plugin : StrategyPlugins.trusted()) {
            StrategyDescriptor descriptor = plugin.descriptor();
            String identity = descriptor.reference().pluginId().value()
                    + "@"
                    + descriptor.reference().implementationVersion();
            assertTrue(identities.add(identity), "Duplicate Strategy identity: " + identity);
        }
    }

    @Test
    void everyTrustedPluginResolvesDefaultsAndCreatesThroughTheSharedContract() {
        StrategyParameterValidator validator = new StrategyParameterValidator();

        for (StrategyPlugin plugin : StrategyPlugins.trusted()) {
            StrategyDescriptor descriptor = plugin.descriptor();
            StrategyParameterSet defaults = validator.resolve(descriptor.parameterSchema(), Map.of());

            assertEquals("strategy-contract-v1", descriptor.contractVersion());
            assertFalse(descriptor.descriptorFingerprint().isBlank());
            assertTrue(plugin.requiredLookback(defaults) >= descriptor.requiredLookback());
            assertNotNull(plugin.create(defaults));
        }
    }

    @Test
    void trustedCompositionCannotBeMutatedByCallers() {
        List<StrategyPlugin> plugins = StrategyPlugins.trusted();

        assertThrows(UnsupportedOperationException.class, () -> plugins.add(plugins.getFirst()));
    }
}

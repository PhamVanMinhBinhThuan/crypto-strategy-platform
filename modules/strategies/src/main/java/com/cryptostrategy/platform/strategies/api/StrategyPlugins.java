package com.cryptostrategy.platform.strategies.api;

import com.cryptostrategy.platform.strategies.internal.bollinger.BollingerBandsPlugin;
import com.cryptostrategy.platform.strategies.internal.ma.MovingAverageCrossoverPlugin;
import com.cryptostrategy.platform.strategies.internal.rsi.RsiPlugin;
import com.cryptostrategy.platform.strategies.internal.support.SupportResistancePlugin;
import com.cryptostrategy.platform.strategy.api.StrategyPlugin;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StrategyPlugins {
    private static final List<StrategyPlugin> TRUSTED = validateUniqueIdentities(List.of(
            new MovingAverageCrossoverPlugin(),
            new RsiPlugin(),
            new BollingerBandsPlugin(),
            new SupportResistancePlugin()));

    private StrategyPlugins() {}

    public static List<StrategyPlugin> trusted() {
        return TRUSTED;
    }

    private static List<StrategyPlugin> validateUniqueIdentities(List<StrategyPlugin> plugins) {
        Set<PluginIdentity> identities = new HashSet<>();
        for (StrategyPlugin plugin : plugins) {
            PluginIdentity identity = new PluginIdentity(
                    plugin.descriptor().reference().pluginId(),
                    plugin.descriptor().reference().implementationVersion());
            if (!identities.add(identity)) {
                throw new IllegalStateException("Duplicate trusted Strategy plugin: " + identity);
            }
        }
        return List.copyOf(plugins);
    }

    private record PluginIdentity(StrategyPluginId pluginId, SemanticVersion version) {}
}

package com.cryptostrategy.platform.strategy.internal.registry;

import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.StrategyPlugin;
import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyDescriptor;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.cryptostrategy.platform.strategy.internal.parameter.StrategyParameterValidator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class DefaultStrategyRegistry implements StrategyRegistry {
    private static final String CONTRACT_VERSION = "strategy-contract-v1";
    private final Map<Key, StrategyPlugin> plugins;
    private final StrategyParameterValidator validator;
    public DefaultStrategyRegistry(List<StrategyPlugin> plugins) { this(plugins, new StrategyParameterValidator()); }
    public DefaultStrategyRegistry(List<StrategyPlugin> contributions, StrategyParameterValidator validator) {
        this.validator = validator; TreeMap<Key, StrategyPlugin> registered = new TreeMap<>();
        for (StrategyPlugin plugin : contributions) {
            StrategyDescriptor descriptor = plugin.descriptor();
            if (!CONTRACT_VERSION.equals(descriptor.contractVersion())) throw new StrategyException(StrategyErrorCode.UNSUPPORTED_VERSION, "Unsupported Strategy contract");
            Key key = new Key(descriptor.reference().pluginId(), descriptor.reference().implementationVersion());
            if (registered.putIfAbsent(key, plugin) != null) throw new StrategyException(StrategyErrorCode.DUPLICATE_REGISTRATION, "Duplicate Strategy registration: " + key);
        }
        this.plugins = Collections.unmodifiableMap(registered);
    }
    @Override public List<StrategyDescriptor> listAvailable() { return plugins.values().stream().map(StrategyPlugin::descriptor).sorted().toList(); }
    @Override public StrategyDescriptor descriptor(StrategyPluginId pluginId, SemanticVersion version) { return plugin(pluginId, version).descriptor(); }
    @Override public StrategyParameterSet resolveParameters(StrategyPluginId pluginId, SemanticVersion version, Map<String, StrategyParameterValue> supplied) {
        return validator.resolve(plugin(pluginId, version).descriptor().parameterSchema(), supplied);
    }
    @Override public Strategy create(StrategyPluginId pluginId, SemanticVersion version, Map<String, StrategyParameterValue> supplied) {
        StrategyPlugin plugin = plugin(pluginId, version); return plugin.create(validator.resolve(plugin.descriptor().parameterSchema(), supplied));
    }
    private StrategyPlugin plugin(StrategyPluginId pluginId, SemanticVersion version) {
        StrategyPlugin plugin = plugins.get(new Key(pluginId, version));
        if (plugin == null) throw new StrategyException(StrategyErrorCode.STRATEGY_NOT_FOUND, "Strategy version is unavailable");
        return plugin;
    }
    private record Key(StrategyPluginId pluginId, SemanticVersion version) implements Comparable<Key> {
        @Override public int compareTo(Key other) { int result=pluginId.compareTo(other.pluginId); return result==0?version.compareTo(other.version):result; }
    }
}

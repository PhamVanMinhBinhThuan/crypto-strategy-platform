package com.cryptostrategy.platform.search.internal;

import com.cryptostrategy.platform.search.api.model.GeneratorDescriptor;
import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.port.in.StrategyGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Registry bất biến, chỉ resolve đúng cặp generator ID/version đã đóng băng. */
public final class StrategyGeneratorRegistry
        implements com.cryptostrategy.platform.search.api.port.in.StrategyGeneratorRegistry {
    private final Map<Key, StrategyGenerator> generators;
    private final List<GeneratorDescriptor> descriptors;

    public StrategyGeneratorRegistry(List<StrategyGenerator> generators) {
        Objects.requireNonNull(generators, "generators");
        List<StrategyGenerator> ordered = new ArrayList<>(generators);
        ordered.forEach(generator -> Objects.requireNonNull(generator, "generator"));
        ordered.sort((left, right) -> {
            int byId = left.descriptor().generatorId().compareTo(right.descriptor().generatorId());
            return byId != 0 ? byId : left.descriptor().generatorVersion()
                    .compareTo(right.descriptor().generatorVersion());
        });

        Map<Key, StrategyGenerator> exact = new LinkedHashMap<>();
        for (StrategyGenerator generator : ordered) {
            GeneratorDescriptor descriptor = Objects.requireNonNull(
                    generator.descriptor(), "generator descriptor");
            Key key = new Key(descriptor.generatorId(), descriptor.generatorVersion());
            if (exact.putIfAbsent(key, generator) != null) {
                throw new IllegalArgumentException("duplicate generator identity: " + key);
            }
        }
        this.generators = Collections.unmodifiableMap(exact);
        this.descriptors = exact.values().stream().map(StrategyGenerator::descriptor).toList();
    }

    @Override
    public Optional<StrategyGenerator> find(GeneratorId generatorId, GeneratorVersion generatorVersion) {
        return Optional.ofNullable(generators.get(new Key(
                Objects.requireNonNull(generatorId, "generatorId"),
                Objects.requireNonNull(generatorVersion, "generatorVersion"))));
    }

    @Override
    public List<GeneratorDescriptor> descriptors() {
        return descriptors;
    }

    private record Key(GeneratorId id, GeneratorVersion version) {}
}

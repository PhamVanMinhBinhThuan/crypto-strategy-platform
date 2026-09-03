package com.cryptostrategy.platform.search.api.port.in;

import com.cryptostrategy.platform.search.api.model.GeneratorDescriptor;
import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import java.util.List;
import java.util.Optional;

/** Exact-version lookup boundary. Implementations must never silently fall back to another version. */
public interface StrategyGeneratorRegistry {
    Optional<StrategyGenerator> find(GeneratorId generatorId, GeneratorVersion generatorVersion);

    List<GeneratorDescriptor> descriptors();

    default StrategyGenerator require(GeneratorId generatorId, GeneratorVersion generatorVersion) {
        return find(generatorId, generatorVersion).orElseThrow(() -> new IllegalArgumentException(
                "Unsupported generator: " + generatorId + "@" + generatorVersion));
    }
}

package com.cryptostrategy.platform.search.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.port.in.StrategyGenerator;
import com.cryptostrategy.platform.search.api.port.in.StrategyGeneratorRegistry;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SearchModuleFactoryTest {
    @Test
    void composesPublishedRegistryAndStatePorts() {
        StrategyGenerator generator = mock(StrategyGenerator.class);
        StrategyGeneratorRegistry registry = new StrategyGeneratorRegistry() {
            @Override
            public Optional<StrategyGenerator> find(GeneratorId id, GeneratorVersion version) {
                return Optional.of(generator);
            }

            @Override
            public java.util.List<com.cryptostrategy.platform.search.api.model.GeneratorDescriptor> descriptors() {
                return java.util.List.of();
            }
        };
        SearchRunStore store = mock(SearchRunStore.class);

        var components = SearchModuleFactory.create(registry, store);

        assertThat(components.searchRuns()).isSameAs(store);
        assertThat(components.requireGenerator(new GeneratorId("random-search"), GeneratorVersion.parse("1.0.0")))
                .isSameAs(generator);
    }
}

package com.cryptostrategy.platform.search.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cryptostrategy.platform.search.api.model.GenerationOutcome;
import com.cryptostrategy.platform.search.api.model.GenerationRequest;
import com.cryptostrategy.platform.search.api.model.GeneratorDescriptor;
import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.port.in.StrategyGenerator;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StrategyGeneratorRegistryTest {
    @Test
    void rejectsDuplicateExactIdentityAndVersion() {
        StrategyGenerator first = generator("fixture", "1.0.0");
        StrategyGenerator duplicate = generator("fixture", "1.0.0");

        assertThatThrownBy(() -> new com.cryptostrategy.platform.search.internal.StrategyGeneratorRegistry(
                List.of(first, duplicate)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void resolvesOnlyTheExactVersionWithoutFallback() {
        StrategyGenerator v1 = generator("fixture", "1.0.0");
        StrategyGenerator v2 = generator("fixture", "2.0.0");
        var registry = new com.cryptostrategy.platform.search.internal.StrategyGeneratorRegistry(List.of(v2, v1));

        assertThat(registry.require(new GeneratorId("fixture"), GeneratorVersion.parse("1.0.0")))
                .isSameAs(v1);
        assertThat(registry.descriptors()).extracting(GeneratorDescriptor::generatorVersion)
                .containsExactly(GeneratorVersion.parse("1.0.0"), GeneratorVersion.parse("2.0.0"));
        assertThatThrownBy(() -> registry.require(
                new GeneratorId("fixture"), GeneratorVersion.parse("1.1.0")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported generator");
    }

    private static StrategyGenerator generator(String id, String version) {
        GeneratorDescriptor descriptor = new GeneratorDescriptor(
                new GeneratorId(id), GeneratorVersion.parse(version), "fixture-state-v1",
                Set.of(ParameterType.INTEGER), id + '-' + version);
        return new StrategyGenerator() {
            @Override
            public GeneratorDescriptor descriptor() {
                return descriptor;
            }

            @Override
            public GenerationOutcome generateNext(GenerationRequest request) {
                throw new UnsupportedOperationException("Not needed by registry contract test");
            }
        };
    }
}

package com.cryptostrategy.platform.search.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.search.api.SearchModuleFactory;
import com.cryptostrategy.platform.search.api.model.GenerationOutcome;
import com.cryptostrategy.platform.search.api.model.GenerationRequest;
import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.search.api.model.SearchSpace;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import com.cryptostrategy.platform.search.fixtures.FixtureStrategyGenerator;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeneratorReplaceabilityTest {
    @Test
    void conformingGeneratorIsRegisteredThroughPublicFactoryAndIsDeterministic() {
        FixtureStrategyGenerator fixture = new FixtureStrategyGenerator();
        SearchRunStore unusedStore = (SearchRunStore) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {SearchRunStore.class},
                (proxy, method, arguments) -> { throw new UnsupportedOperationException(); });
        var components = SearchModuleFactory.fromGenerators(List.of(fixture), unusedStore);
        var resolved = components.requireGenerator(new GeneratorId("fixture-search"), GeneratorVersion.parse("1.0.0"));
        GenerationRequest request = GenerationRequest.initial(new SearchSpace(Map.of(
                "period", new SearchParameterDomain(ParameterType.INTEGER,
                        List.of(new StrategyParameterValue.IntegerValue(5),
                                new StrategyParameterValue.IntegerValue(10))))), 17L, 4);

        GenerationOutcome first = resolved.generateNext(request);
        GenerationOutcome replay = resolved.generateNext(request);

        assertThat(replay).isEqualTo(first);
        assertThat(first).isInstanceOf(GenerationOutcome.Generated.class);
        assertThat(components.generators().descriptors()).containsExactly(fixture.descriptor());
    }
}

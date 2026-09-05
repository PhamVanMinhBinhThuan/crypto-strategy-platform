package com.cryptostrategy.platform.search.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.search.api.model.CompositeSearchSpace;
import com.cryptostrategy.platform.search.api.model.GenerationOutcome;
import com.cryptostrategy.platform.search.api.model.GenerationRequest;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.SearchCombinationPolicy;
import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.search.api.model.SearchStrategyPoolEntry;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CompositeRandomStrategyGeneratorTest {
    @Test
    void traversesCompositeDefinitionsDeterministicallyAndExhaustsWithoutMaterializingTheSpace() {
        CompositeSearchSpace space = new CompositeSearchSpace(
                List.of(entry("alpha", "01J00000000000000000000001", 12),
                        entry("beta", "01J00000000000000000000002", 12)),
                1, 2, SearchCombinationPolicy.majorityVote(), List.of());
        var generator = new RandomStrategyGenerator();
        var guard = new SearchGenerationService();
        Set<String> accepted = new HashSet<>();
        Optional<GeneratorState> state = Optional.empty();

        int count = space.combinationCount().intValueExact();
        for (int index = 0; index < count; index++) {
            var outcome = guard.generateNext(generator,
                    GenerationRequest.composite(space, 73021L, state, index, accepted, 4));
            var generated = (GenerationOutcome.Generated) outcome;
            assertThat(generated.candidate().compositeDefinition()).isPresent();
            assertThat(generated.candidate().generationIndex()).isEqualTo(index);
            accepted.add(generated.candidate().fingerprint());
            state = Optional.of(generated.nextState());
        }

        assertThat(accepted).hasSize(count);
        assertThat(guard.generateNext(generator,
                GenerationRequest.composite(space, 73021L, state, count, accepted, 4)))
                .isInstanceOf(GenerationOutcome.Exhausted.class);
    }

    private static SearchStrategyPoolEntry entry(String plugin, String versionId, int size) {
        List<StrategyParameterValue> options = java.util.stream.LongStream.range(0, size)
                .mapToObj(StrategyParameterValue.IntegerValue::new)
                .map(StrategyParameterValue.class::cast).toList();
        return new SearchStrategyPoolEntry(
                new StrategyReference(new StrategyVersionId(versionId), new StrategyPluginId(plugin),
                        SemanticVersion.parse("1.0.0")),
                Map.of("period", new SearchParameterDomain(ParameterType.INTEGER, options)));
    }
}

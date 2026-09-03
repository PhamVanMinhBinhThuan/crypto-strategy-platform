package com.cryptostrategy.platform.search.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.search.api.model.GenerationOutcome;
import com.cryptostrategy.platform.search.api.model.GenerationRequest;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.search.api.model.SearchSpace;
import com.cryptostrategy.platform.search.api.port.in.StrategyGenerator;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RandomStrategyGeneratorTest {
    @Test
    void sameSeedAndCanonicalSpaceProduceTheSameSequenceAndState() {
        var generator = new RandomStrategyGenerator();

        List<GenerationOutcome.Generated> first = generate(generator, searchSpaceForward(), 73021L, 4);
        List<GenerationOutcome.Generated> replay = generate(generator, searchSpaceReverse(), 73021L, 4);

        assertThat(replay).isEqualTo(first);
        assertThat(first).extracting(item -> item.candidate().generationIndex())
                .containsExactly(0, 1, 2, 3);
        assertThat(first.getLast().nextState().contractVersion()).isEqualTo("random-state-v1");
    }

    @Test
    void differentSeedsProduceDifferentDeterministicSequences() {
        var generator = new RandomStrategyGenerator();

        List<String> first = generate(generator, searchSpaceForward(), 101L, 5).stream()
                .map(item -> item.candidate().fingerprint()).toList();
        List<String> second = generate(generator, searchSpaceForward(), 202L, 5).stream()
                .map(item -> item.candidate().fingerprint()).toList();

        assertThat(second).isNotEqualTo(first);
    }

    private static List<GenerationOutcome.Generated> generate(
            StrategyGenerator generator,
            SearchSpace searchSpace,
            long seed,
            int count
    ) {
        List<GenerationOutcome.Generated> generated = new ArrayList<>();
        Optional<GeneratorState> state = Optional.empty();
        Set<String> accepted = new HashSet<>();
        for (int index = 0; index < count; index++) {
            GenerationRequest request = new GenerationRequest(
                    searchSpace, seed, state, index, accepted, 100);
            GenerationOutcome.Generated outcome = (GenerationOutcome.Generated) generator.generateNext(request);
            generated.add(outcome);
            state = Optional.of(outcome.nextState());
            accepted.add(outcome.candidate().fingerprint());
        }
        return generated;
    }

    private static SearchSpace searchSpaceForward() {
        return new SearchSpace(Map.of(
                "period", integers(5, 10, 20, 50),
                "signal", enums("ema", "macd", "rsi"),
                "enabled", booleans(true, false)));
    }

    private static SearchSpace searchSpaceReverse() {
        Map<String, SearchParameterDomain> parameters = new HashMap<>();
        parameters.put("signal", enums("rsi", "macd", "ema"));
        parameters.put("enabled", booleans(false, true));
        parameters.put("period", integers(50, 20, 10, 5));
        return new SearchSpace(parameters);
    }

    private static SearchParameterDomain integers(long... values) {
        return new SearchParameterDomain(ParameterType.INTEGER,
                java.util.Arrays.stream(values)
                        .mapToObj(StrategyParameterValue.IntegerValue::new)
                        .map(StrategyParameterValue.class::cast)
                        .toList());
    }

    private static SearchParameterDomain enums(String... values) {
        return new SearchParameterDomain(ParameterType.ENUM,
                java.util.Arrays.stream(values)
                        .map(StrategyParameterValue.EnumValue::new)
                        .map(StrategyParameterValue.class::cast)
                        .toList());
    }

    private static SearchParameterDomain booleans(boolean... values) {
        List<StrategyParameterValue> options = new ArrayList<>();
        for (boolean value : values) {
            options.add(new StrategyParameterValue.BooleanValue(value));
        }
        return new SearchParameterDomain(ParameterType.BOOLEAN, options);
    }
}

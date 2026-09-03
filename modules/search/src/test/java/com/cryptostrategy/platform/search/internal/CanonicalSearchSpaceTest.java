package com.cryptostrategy.platform.search.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.search.api.model.SearchSpace;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanonicalSearchSpaceTest {
    @Test
    void insertionOrderDoesNotChangeTheFingerprint() {
        Map<String, SearchParameterDomain> forward = new LinkedHashMap<>();
        forward.put("period", integers(5, 10));
        forward.put("signal", enums("ema", "rsi"));
        Map<String, SearchParameterDomain> reverse = new LinkedHashMap<>();
        reverse.put("signal", enums("rsi", "ema"));
        reverse.put("period", integers(10, 5));

        assertThat(CanonicalSearchSpace.fingerprint(new SearchSpace(reverse)))
                .isEqualTo(CanonicalSearchSpace.fingerprint(new SearchSpace(forward)))
                .startsWith("sha256:");
    }

    @Test
    void validatesTheExactFrozenParameterDomain() {
        SearchSpace space = new SearchSpace(Map.of("period", integers(5, 10)));

        assertThat(CanonicalSearchSpace.contains(space, parameters(5))).isTrue();
        assertThat(CanonicalSearchSpace.contains(space, parameters(99))).isFalse();
        assertThat(CanonicalSearchSpace.contains(
                        space,
                        StrategyParameterSet.of(Map.of(
                                "period", new StrategyParameterValue.IntegerValue(5),
                                "extra", new StrategyParameterValue.IntegerValue(1)))))
                .isFalse();
    }

    @Test
    void candidateFingerprintIsCanonicalAndTypeAware() {
        StrategyParameterSet first = StrategyParameterSet.of(Map.of(
                "period", new StrategyParameterValue.IntegerValue(5),
                "signal", new StrategyParameterValue.EnumValue("ema")));
        StrategyParameterSet replay = StrategyParameterSet.of(new LinkedHashMap<>(Map.of(
                "signal", new StrategyParameterValue.EnumValue("ema"),
                "period", new StrategyParameterValue.IntegerValue(5))));

        assertThat(CanonicalSearchSpace.candidateFingerprint(replay))
                .isEqualTo(CanonicalSearchSpace.candidateFingerprint(first));
    }

    private static StrategyParameterSet parameters(long period) {
        return StrategyParameterSet.of(Map.of(
                "period", new StrategyParameterValue.IntegerValue(period)));
    }

    private static SearchParameterDomain integers(long... values) {
        return new SearchParameterDomain(
                ParameterType.INTEGER,
                java.util.Arrays.stream(values)
                        .mapToObj(StrategyParameterValue.IntegerValue::new)
                        .map(StrategyParameterValue.class::cast)
                        .toList());
    }

    private static SearchParameterDomain enums(String... values) {
        return new SearchParameterDomain(
                ParameterType.ENUM,
                java.util.Arrays.stream(values)
                        .map(StrategyParameterValue.EnumValue::new)
                        .map(StrategyParameterValue.class::cast)
                        .toList());
    }
}

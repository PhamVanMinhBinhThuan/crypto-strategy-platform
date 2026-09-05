package com.cryptostrategy.platform.search.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cryptostrategy.platform.search.api.model.CompositeSearchSpace;
import com.cryptostrategy.platform.search.api.model.SearchCombinationPolicy;
import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.search.api.model.SearchStrategyPoolEntry;
import com.cryptostrategy.platform.search.internal.CanonicalCompositeSearchSpace;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CompositeSearchModelContractTest {
    @Test
    void canonicalPoolCountsSubsetsAndParameterProducts() {
        var first = entry("alpha", "01J00000000000000000000001", 2);
        var second = entry("beta", "01J00000000000000000000002", 3);
        var space = new CompositeSearchSpace(
                List.of(second, first), 1, 2, SearchCombinationPolicy.majorityVote(), List.of());

        assertThat(space.strategyPool()).containsExactly(first, second);
        assertThat(space.combinationCount()).isEqualTo(BigInteger.valueOf(11));
        assertThat(CanonicalCompositeSearchSpace.fingerprint(space)).startsWith("sha256:");
    }

    @Test
    void rejectsDuplicateVersionsAndImpossibleComponentBounds() {
        var entry = entry("alpha", "01J00000000000000000000001", 2);
        assertThatThrownBy(() -> new CompositeSearchSpace(
                List.of(entry, entry), 1, 2, SearchCombinationPolicy.majorityVote(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CompositeSearchSpace(
                List.of(entry), 2, 2, SearchCombinationPolicy.majorityVote(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void seededTraversalIsUniqueRepeatableAndExhaustsExactly() {
        var space = new CompositeSearchSpace(
                List.of(entry("alpha", "01J00000000000000000000001", 10),
                        entry("beta", "01J00000000000000000000002", 10)),
                1, 2, SearchCombinationPolicy.majorityVote(), List.of());
        int count = space.combinationCount().intValueExact();
        HashSet<String> fingerprints = new HashSet<>();
        for (int index = 0; index < count; index++) {
            var candidate = CanonicalCompositeSearchSpace.generate(space, 42L, index).orElseThrow();
            fingerprints.add(candidate.fingerprint());
            assertThat(CanonicalCompositeSearchSpace.generate(space, 42L, index).orElseThrow())
                    .isEqualTo(candidate);
        }
        assertThat(fingerprints).hasSize(count);
        assertThat(CanonicalCompositeSearchSpace.generate(space, 42L, count)).isEmpty();
        List<String> seed42 = java.util.stream.IntStream.range(0, 10)
                .mapToObj(index -> CanonicalCompositeSearchSpace.generate(space, 42L, index)
                        .orElseThrow().fingerprint())
                .toList();
        List<String> seed43 = java.util.stream.IntStream.range(0, 10)
                .mapToObj(index -> CanonicalCompositeSearchSpace.generate(space, 43L, index)
                        .orElseThrow().fingerprint())
                .toList();
        assertThat(seed43).isNotEqualTo(seed42);
    }

    private static SearchStrategyPoolEntry entry(String plugin, String versionId, int options) {
        List<StrategyParameterValue> values = java.util.stream.LongStream.range(0, options)
                .mapToObj(StrategyParameterValue.IntegerValue::new)
                .map(StrategyParameterValue.class::cast)
                .toList();
        return new SearchStrategyPoolEntry(
                new StrategyReference(new StrategyVersionId(versionId), new StrategyPluginId(plugin),
                        SemanticVersion.parse("1.0.0")),
                Map.of("period", new SearchParameterDomain(ParameterType.INTEGER, values)));
    }
}

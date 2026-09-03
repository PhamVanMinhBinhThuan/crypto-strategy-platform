package com.cryptostrategy.platform.search.api;

import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.port.in.StrategyGenerator;
import com.cryptostrategy.platform.search.api.port.in.StrategyGeneratorRegistry;
import com.cryptostrategy.platform.search.api.port.in.SearchGenerationUseCase;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import java.util.Objects;
import java.util.List;
import java.util.Collection;
import com.cryptostrategy.platform.search.internal.RandomStrategyGenerator;
import com.cryptostrategy.platform.search.internal.SearchGenerationService;
import com.cryptostrategy.platform.search.api.model.GeneratorDescriptor;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.SearchSpace;
import com.cryptostrategy.platform.search.internal.CanonicalSearchSpace;

/** Framework-independent composition root for Search application components. */
public final class SearchModuleFactory {
    private SearchModuleFactory() {
    }

    public static Components create(StrategyGeneratorRegistry generators, SearchRunStore searchRuns) {
        return new Components(generators, searchRuns);
    }

    public static Components baseline(SearchRunStore searchRuns) {
        return fromGenerators(List.of(new RandomStrategyGenerator()), searchRuns);
    }

    /** Frozen metadata/state needed by an application orchestrator before a run is persisted. */
    public static BaselineDefinition baselineDefinition(long seed) {
        RandomStrategyGenerator generator = new RandomStrategyGenerator();
        return new BaselineDefinition(generator.descriptor(), RandomStrategyGenerator.initialState(seed));
    }

    public static String canonicalSearchSpaceFingerprint(SearchSpace searchSpace) {
        return CanonicalSearchSpace.fingerprint(searchSpace);
    }

    public record BaselineDefinition(GeneratorDescriptor descriptor, GeneratorState initialState) {}

    /** Public extension point: composition roots register conforming generators without changing Search internals. */
    public static Components fromGenerators(
            Collection<? extends StrategyGenerator> generators,
            SearchRunStore searchRuns) {
        Objects.requireNonNull(generators, "generators");
        return create(
                new com.cryptostrategy.platform.search.internal.StrategyGeneratorRegistry(
                        List.copyOf(generators)),
                searchRuns);
    }

    public record Components(StrategyGeneratorRegistry generators, SearchRunStore searchRuns) {
        public Components {
            Objects.requireNonNull(generators, "generators");
            Objects.requireNonNull(searchRuns, "searchRuns");
        }

        public StrategyGenerator requireGenerator(GeneratorId generatorId, GeneratorVersion version) {
            return generators.require(generatorId, version);
        }

        public SearchGenerationUseCase generation() {
            SearchGenerationService guard = new SearchGenerationService();
            return (generatorId, version, request) ->
                    guard.generateNext(generators.require(generatorId, version), request);
        }
    }
}

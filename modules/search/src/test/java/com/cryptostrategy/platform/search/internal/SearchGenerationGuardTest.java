package com.cryptostrategy.platform.search.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.search.api.model.GeneratedCandidate;
import com.cryptostrategy.platform.search.api.model.GenerationOutcome;
import com.cryptostrategy.platform.search.api.model.GenerationRequest;
import com.cryptostrategy.platform.search.api.model.GeneratorDescriptor;
import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.search.api.model.SearchSpace;
import com.cryptostrategy.platform.search.api.port.in.StrategyGenerator;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class SearchGenerationGuardTest {
    private final SearchGenerationService service = new SearchGenerationService();
    private final GeneratorState state0 = new GeneratorState("fixture-state-v1", "{\"draw\":0}", "state-0");
    private final GeneratorState state1 = new GeneratorState("fixture-state-v1", "{\"draw\":1}", "state-1");

    @Test
    void rejectsCandidateOutsideFrozenSearchSpace() {
        GenerationOutcome outcome = service.generateNext(
                generator(request -> generated(99, request.expectedGenerationIndex(), "outside", state1)),
                request(Set.of(), 3));

        assertThat(outcome).isEqualTo(new GenerationOutcome.Rejected("OUTPUT_OUTSIDE_SEARCH_SPACE"));
    }

    @Test
    void rejectsNonMonotonicGenerationIndex() {
        GenerationOutcome outcome = service.generateNext(
                generator(request -> generated(5, request.expectedGenerationIndex() + 1, "wrong-index", state1)),
                request(Set.of(), 3));

        assertThat(outcome).isEqualTo(new GenerationOutcome.Rejected("GENERATION_INDEX_MISMATCH"));
    }

    @Test
    void boundsDuplicateDrawsAndReportsNoProgress() {
        GenerationOutcome outcome = service.generateNext(
                generator(request -> generated(5, request.expectedGenerationIndex(), "duplicate", state1)),
                request(Set.of("duplicate"), 1));

        assertThat(outcome).isInstanceOfSatisfying(GenerationOutcome.NoProgress.class,
                value -> assertThat(value.reasonCode()).isEqualTo("DUPLICATE_DRAW_BUDGET_EXHAUSTED"));
    }

    @Test
    void rejectsGeneratorThatDoesNotAdvanceVersionedState() {
        GenerationOutcome outcome = service.generateNext(
                generator(request -> generated(5, request.expectedGenerationIndex(), "candidate", state0)),
                request(Set.of(), 3));

        assertThat(outcome).isEqualTo(new GenerationOutcome.NoProgress(
                state0, "GENERATOR_STATE_DID_NOT_PROGRESS"));
    }

    @Test
    void returnsExhaustedWithoutDrawingWhenAllCanonicalCombinationsExist() {
        String onlyCandidate = CanonicalSearchSpace.candidateFingerprint(
                StrategyParameterSet.of(Map.of(
                        "period", new StrategyParameterValue.IntegerValue(5))));
        GenerationOutcome outcome = service.generateNext(
                generator(request -> { throw new AssertionError("generator must not be called"); }),
                request(Set.of(onlyCandidate), 3));

        assertThat(outcome).isEqualTo(new GenerationOutcome.Exhausted(state0));
    }

    private GenerationRequest request(Set<String> accepted, int budget) {
        return new GenerationRequest(space(), 7L, Optional.of(state0), 0, accepted, budget);
    }

    private static SearchSpace space() {
        return new SearchSpace(Map.of("period", new SearchParameterDomain(
                ParameterType.INTEGER, List.of(new StrategyParameterValue.IntegerValue(5)))));
    }

    private static GenerationOutcome.Generated generated(
            long period,
            int index,
            String fingerprint,
            GeneratorState nextState
    ) {
        return new GenerationOutcome.Generated(
                new GeneratedCandidate(
                        StrategyParameterSet.of(Map.of(
                                "period", new StrategyParameterValue.IntegerValue(period))),
                        index,
                        fingerprint),
                nextState);
    }

    private static StrategyGenerator generator(Function<GenerationRequest, GenerationOutcome> behavior) {
        GeneratorDescriptor descriptor = new GeneratorDescriptor(
                new GeneratorId("fixture"), GeneratorVersion.parse("1.0.0"), "fixture-state-v1",
                Set.of(ParameterType.INTEGER), "fixture-descriptor");
        return new StrategyGenerator() {
            @Override
            public GeneratorDescriptor descriptor() {
                return descriptor;
            }

            @Override
            public GenerationOutcome generateNext(GenerationRequest request) {
                return behavior.apply(request);
            }
        };
    }
}

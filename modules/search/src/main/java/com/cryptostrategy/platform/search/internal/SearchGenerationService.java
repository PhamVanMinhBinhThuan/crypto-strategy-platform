package com.cryptostrategy.platform.search.internal;

import com.cryptostrategy.platform.search.api.model.GenerationOutcome;
import com.cryptostrategy.platform.search.api.model.GenerationRequest;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.port.in.StrategyGenerator;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;

/** Guard thuần bao quanh generator không tin cậy bằng draw budget hữu hạn. */
public final class SearchGenerationService {
    public GenerationOutcome generateNext(StrategyGenerator generator, GenerationRequest request) {
        Objects.requireNonNull(generator, "generator");
        Objects.requireNonNull(request, "request");
        if (allCombinationsAccepted(request)) {
            return new GenerationOutcome.Exhausted(currentState(generator, request));
        }

        GenerationRequest draw = request;
        for (int attempt = 0; attempt < request.remainingDrawBudget(); attempt++) {
            GenerationOutcome raw = Objects.requireNonNull(
                    generator.generateNext(draw), "generator outcome");
            if (!(raw instanceof GenerationOutcome.Generated generated)) {
                return raw;
            }
            if (request.compositeSearchSpace().isEmpty()
                    && !CanonicalSearchSpace.contains(request.searchSpace(), generated.candidate().parameters())) {
                return new GenerationOutcome.Rejected("OUTPUT_OUTSIDE_SEARCH_SPACE");
            }
            if (generated.candidate().generationIndex() != request.expectedGenerationIndex()) {
                return new GenerationOutcome.Rejected("GENERATION_INDEX_MISMATCH");
            }
            GeneratorState previous = draw.priorState().orElse(null);
            if (previous != null && previous.fingerprint().equals(generated.nextState().fingerprint())) {
                return new GenerationOutcome.NoProgress(
                        generated.nextState(), "GENERATOR_STATE_DID_NOT_PROGRESS");
            }
            if (!request.acceptedCandidateFingerprints().contains(generated.candidate().fingerprint())) {
                String canonicalFingerprint = request.compositeSearchSpace()
                        .map(space -> CanonicalCompositeSearchSpace.candidateFingerprint(
                                generated.candidate().compositeDefinition().orElseThrow().components(), space))
                        .orElseGet(() -> CanonicalSearchSpace.candidateFingerprint(
                                generated.candidate().parameters()));
                if (!canonicalFingerprint.equals(generated.candidate().fingerprint())) {
                    return new GenerationOutcome.Rejected("CANDIDATE_FINGERPRINT_MISMATCH");
                }
                return generated;
            }
            if (attempt + 1 == request.remainingDrawBudget()) {
                return new GenerationOutcome.NoProgress(
                        generated.nextState(), "DUPLICATE_DRAW_BUDGET_EXHAUSTED");
            }
            draw = new GenerationRequest(
                    request.searchSpace(),
                    request.compositeSearchSpace(),
                    request.seed(),
                    Optional.of(generated.nextState()),
                    request.expectedGenerationIndex(),
                    request.acceptedCandidateFingerprints(),
                    request.remainingDrawBudget() - attempt - 1);
        }
        throw new IllegalStateException("bounded generation loop terminated unexpectedly");
    }

    private static boolean allCombinationsAccepted(GenerationRequest request) {
        BigInteger acceptedCount = BigInteger.valueOf(request.acceptedCandidateFingerprints().size());
        BigInteger combinationCount = request.compositeSearchSpace()
                .map(com.cryptostrategy.platform.search.api.model.CompositeSearchSpace::combinationCount)
                .orElseGet(() -> request.searchSpace().combinationCount());
        if (acceptedCount.compareTo(combinationCount) < 0) {
            return false;
        }
        if (request.compositeSearchSpace().isPresent()) {
            return true;
        }
        List<java.util.Map.Entry<String, SearchParameterDomain>> domains =
                new ArrayList<>(request.searchSpace().parameters().entrySet());
        return allCombinationsAccepted(domains, 0, new TreeMap<>(),
                request.acceptedCandidateFingerprints());
    }

    private static boolean allCombinationsAccepted(
            List<java.util.Map.Entry<String, SearchParameterDomain>> domains,
            int position,
            TreeMap<String, StrategyParameterValue> values,
            Set<String> accepted) {
        if (position == domains.size()) {
            return accepted.contains(CanonicalSearchSpace.candidateFingerprint(
                    StrategyParameterSet.of(values)));
        }
        var domain = domains.get(position);
        for (StrategyParameterValue option : domain.getValue().options()) {
            values.put(domain.getKey(), option);
            if (!allCombinationsAccepted(domains, position + 1, values, accepted)) {
                values.remove(domain.getKey());
                return false;
            }
        }
        values.remove(domain.getKey());
        return true;
    }

    private static GeneratorState currentState(
            StrategyGenerator generator,
            GenerationRequest request) {
        return request.priorState().orElseGet(() -> new GeneratorState(
                generator.descriptor().stateContractVersion(),
                "{\"status\":\"initial\"}",
                "initial:" + request.seed()));
    }
}

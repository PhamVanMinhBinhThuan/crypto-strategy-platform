package com.cryptostrategy.platform.search.fixtures;

import com.cryptostrategy.platform.search.api.model.GeneratedCandidate;
import com.cryptostrategy.platform.search.api.model.GenerationOutcome;
import com.cryptostrategy.platform.search.api.model.GenerationRequest;
import com.cryptostrategy.platform.search.api.model.GeneratorDescriptor;
import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.port.in.StrategyGenerator;
import com.cryptostrategy.platform.search.internal.CanonicalSearchSpace;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

/** Generator fixture thuần, chọn option theo chỉ số để chứng minh khả năng thay thế. */
public final class FixtureStrategyGenerator implements StrategyGenerator {
    private static final GeneratorDescriptor DESCRIPTOR = new GeneratorDescriptor(
            new GeneratorId("fixture-search"), GeneratorVersion.parse("1.0.0"),
            "fixture-state-v1", EnumSet.allOf(ParameterType.class), "fixture-search:1.0.0:v1");

    @Override
    public GeneratorDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public GenerationOutcome generateNext(GenerationRequest request) {
        int index = request.expectedGenerationIndex();
        Map<String, com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue> values =
                new TreeMap<>();
        request.searchSpace().parameters().forEach((name, domain) ->
                values.put(name, domain.options().get(index % domain.options().size())));
        StrategyParameterSet parameters = StrategyParameterSet.of(values);
        String fingerprint = CanonicalSearchSpace.candidateFingerprint(parameters);
        GeneratorState state = new GeneratorState(
                DESCRIPTOR.stateContractVersion(),
                "{\"nextIndex\":" + (index + 1) + "}",
                "fixture-state:" + (index + 1));
        return new GenerationOutcome.Generated(
                new GeneratedCandidate(parameters, index, fingerprint), state);
    }
}

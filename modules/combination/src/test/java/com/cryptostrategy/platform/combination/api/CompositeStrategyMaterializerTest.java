package com.cryptostrategy.platform.combination.api;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.combination.internal.DefaultCompositeStrategyMaterializer;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CompositeStrategyMaterializerTest {
    @Test void rejectsACompositeWithFewerThanTwoOrderedComponents() {
        var policyRef = new CombinationPolicyReference(new CombinationPolicyId("majority"), new SemanticVersion(1,0,0));
        CombinationPolicy policy = new CombinationPolicy() {
            public CombinationPolicyReference reference() { return policyRef; }
            public StrategySignal combine(List<StrategyDecision> decisions) { return StrategySignal.HOLD; }
        };
        Strategy component = context -> null;
        var materializer = new DefaultCompositeStrategyMaterializer(List.of(policy));
        var reference = new StrategyReference(new StrategyVersionId("01J00000000000000000000000"),
                new StrategyPluginId("composite"), new SemanticVersion(1,0,0));
        assertThrows(IllegalArgumentException.class,
                () -> materializer.materialize(reference, policyRef, List.of(component)));
    }
}

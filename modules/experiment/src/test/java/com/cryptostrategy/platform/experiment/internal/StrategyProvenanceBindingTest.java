package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.provenance.StrategyComponentSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.strategy.api.model.CombinationPolicyId;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyKind;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class StrategyProvenanceBindingTest {

    @Test
    @DisplayName("Single strategy provenance snapshot encapsulates single strategy fields")
    void singleStrategyBinding() {
        StrategyProvenanceSnapshot single = ProvenanceTestFixtures.single(
                "moving-average",
                "1.0.0",
                Map.of("fastPeriod", 10, "slowPeriod", 20),
                "01ARZ3NDEKTSV4RRFFQ69G5FAV"
        );

        assertThat(single.kind()).isEqualTo(StrategyKind.SINGLE);
        assertThat(single.singleStrategy().orElseThrow().pluginId().value()).isEqualTo("moving-average");
        assertThat(single.singleStrategy().orElseThrow().implementationVersion().toString()).isEqualTo("1.0.0");
        assertThat(single.parameters().require("fastPeriod").canonicalText()).isEqualTo("10");
        assertThat(single.sourceUserStrategyVersionId()).contains(new UserStrategyVersionId("01ARZ3NDEKTSV4RRFFQ69G5FAV"));
        assertThat(single.strategyFingerprint()).isEqualTo(ProvenanceTestFixtures.STRATEGY_FINGERPRINT);
    }

    @Test
    @DisplayName("Composite strategy provenance snapshot encapsulates ordered components and weights")
    void compositeStrategyBinding() {
        StrategyComponentSnapshot c1 = new StrategyComponentSnapshot(ProvenanceTestFixtures.reference("sma"), ProvenanceTestFixtures.parameters(Map.of()));
        StrategyComponentSnapshot c2 = new StrategyComponentSnapshot(ProvenanceTestFixtures.reference("rsi"), ProvenanceTestFixtures.parameters(Map.of()));

        StrategyProvenanceSnapshot composite = StrategyProvenanceSnapshot.composite(
                new CombinationPolicyId("majority-vote"),
                new SemanticVersion(1, 0, 0),
                ProvenanceTestFixtures.parameters(Map.of()),
                List.of(c1, c2),
                Optional.of(new UserStrategyVersionId("01ARZ3NDEKTSV4RRFFQ69G5FAW")),
                ProvenanceTestFixtures.STRATEGY_FINGERPRINT
        );

        assertThat(composite.kind()).isEqualTo(StrategyKind.COMPOSITE);
        assertThat(composite.components()).hasSize(2);
        assertThat(composite.compositePolicyId()).contains(new CombinationPolicyId("majority-vote"));
        assertThat(composite.compositePolicyVersion()).contains(new SemanticVersion(1, 0, 0));
    }
}

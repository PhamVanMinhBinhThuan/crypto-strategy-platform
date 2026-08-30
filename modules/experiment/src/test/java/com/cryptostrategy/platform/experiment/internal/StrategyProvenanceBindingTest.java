package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.provenance.StrategyComponentSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StrategyProvenanceBindingTest {

    @Test
    @DisplayName("Single strategy provenance snapshot encapsulates single strategy fields")
    void singleStrategyBinding() {
        StrategyProvenanceSnapshot single = StrategyProvenanceSnapshot.single(
                "moving-average",
                "1.0.0",
                Map.of("fastPeriod", 10, "slowPeriod", 20),
                "01ARZ3NDEKTSV4RRFFQ69G5FAV"
        );

        assertThat(single.strategyKind()).isEqualTo("SINGLE");
        assertThat(single.strategyRefId()).isEqualTo("moving-average");
        assertThat(single.strategyVersion()).isEqualTo("1.0.0");
        assertThat(single.parameters()).containsEntry("fastPeriod", 10);
        assertThat(single.sourceUserStrategyVersionId()).isEqualTo("01ARZ3NDEKTSV4RRFFQ69G5FAV");
    }

    @Test
    @DisplayName("Composite strategy provenance snapshot encapsulates ordered components and weights")
    void compositeStrategyBinding() {
        StrategyComponentSnapshot c1 = new StrategyComponentSnapshot(
                "sma", "1.0", Map.of(), new BigDecimal("0.6"), 0
        );
        StrategyComponentSnapshot c2 = new StrategyComponentSnapshot(
                "rsi", "1.0", Map.of(), new BigDecimal("0.4"), 1
        );

        StrategyProvenanceSnapshot composite = StrategyProvenanceSnapshot.composite(
                "composite-trend",
                "1.0.0",
                "MAJORITY",
                Map.of(),
                List.of(c1, c2),
                "01ARZ3NDEKTSV4RRFFQ69G5FAW"
        );

        assertThat(composite.strategyKind()).isEqualTo("COMPOSITE");
        assertThat(composite.components()).hasSize(2);
        assertThat(composite.compositePolicyId()).isEqualTo("MAJORITY");
    }
}

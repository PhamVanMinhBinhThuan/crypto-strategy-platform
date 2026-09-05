package com.cryptostrategy.platform.api.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.execution.api.port.in.SearchStartCommandFactory;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchExperimentUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CompositeSearchRequestMapperTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final UUID owner = UUID.fromString("91000000-0000-4000-8000-000000000015");

    @Test
    void mapsTypedV2PoolBoundsConstraintsAndAllStopConditions() throws Exception {
        var request = json.readValue("""
                {
                  "configurationVersion": 2,
                  "name": "Composite Search",
                  "datasetId": "01J00000000000000000000401",
                  "backtestConfiguration": {
                    "initialCapital": "25000.50",
                    "feeRate": "0.001",
                    "slippageRate": "0.0005"
                  },
                  "generator": {"generatorId": "random-search", "version": "1.0.0", "seed": 42},
                  "searchSpace": {
                    "schemaVersion": 2,
                    "strategyPool": [{
                      "artifactType": "BUILT_IN",
                      "strategyId": "rsi",
                      "version": "1.0.0",
                      "parameterDomains": {
                        "period": {"kind": "INTEGER_RANGE", "min": 7, "max": 21, "step": 7},
                        "buyThreshold": {"kind": "DECIMAL_RANGE", "min": 20.5, "max": 30.5, "step": 0.5}
                      }
                    }],
                    "minComponents": 1,
                    "maxComponents": 1,
                    "combinationPolicy": {"policyId": "majority-vote", "version": "1.0.0"},
                    "constraints": [{"kind": "PARAMETER_LT", "left": "rsi.buyThreshold", "right": "rsi.sellThreshold"}]
                  },
                  "stopConditions": {
                    "maximumCandidates": 100,
                    "maximumDurationSeconds": 300,
                    "maximumWithoutImprovement": 25
                  },
                  "topK": 10,
                  "requestedConcurrency": 4
                }
                """, CommandDtos.StartExperimentRequest.class);
        SearchStartCommandFactory factory = mock(SearchStartCommandFactory.class);
        when(factory.create(any())).thenReturn(mock(StartSearchExperimentUseCase.StartCommand.class));

        new ExperimentRequestMapper(factory).map(owner, "key", "hash", "correlation", request);

        ArgumentCaptor<SearchStartCommandFactory.Request> capture =
                ArgumentCaptor.forClass(SearchStartCommandFactory.Request.class);
        verify(factory).create(capture.capture());
        var mapped = capture.getValue();
        assertThat(mapped.maximumWithoutImprovement()).isEqualTo(25);
        assertThat(mapped.requestedConcurrency()).isEqualTo(4);
        assertThat(mapped.backtestAssumptions().initialCapital())
                .isEqualByComparingTo(new BigDecimal("25000.50"));
        assertThat(mapped.backtestAssumptions().feeRate())
                .isEqualByComparingTo(new BigDecimal("0.001"));
        assertThat(mapped.backtestAssumptions().slippageRate())
                .isEqualByComparingTo(new BigDecimal("0.0005"));
        assertThat(mapped.strategyPool()).hasSize(1);
        assertThat(mapped.strategyPool().getFirst().parameters().get("buyThreshold").kind())
                .isEqualTo("DECIMAL_RANGE");
        assertThat(mapped.strategyPool().getFirst().parameters().get("buyThreshold").step())
                .isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(mapped.constraints()).hasSize(1);
    }

    @Test
    void rejectsV2WithoutTheV2SearchSpaceDiscriminatorBeforeCallingFactory() throws Exception {
        var request = json.readValue("""
                {
                  "configurationVersion": 2,
                  "datasetId": "01J00000000000000000000401",
                  "searchSpace": {"schemaVersion": 1}
                }
                """, CommandDtos.StartExperimentRequest.class);
        SearchStartCommandFactory factory = mock(SearchStartCommandFactory.class);

        assertThatThrownBy(() -> new ExperimentRequestMapper(factory)
                .map(owner, "key", "hash", "correlation", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("schemaVersion 2");
    }
}

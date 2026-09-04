package com.cryptostrategy.platform.api.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.execution.api.port.in.SearchStartCommandFactory;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchExperimentUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ExperimentUserStrategyRequestMapperTest {
    @Test
    void mapsPublishedUserStrategyVersionWithAuthenticatedOwner() throws Exception {
        String versionId = "01J00000000000000000000402";
        CommandDtos.StartExperimentRequest request = new ObjectMapper().readValue("""
                {
                  "name": "Composite demo",
                  "datasetId": "01J00000000000000000000401",
                  "generator": {
                    "generatorId": "random-search",
                    "version": "1.0.0",
                    "seed": 14
                  },
                  "userStrategyVersionId": "%s",
                  "stopCondition": {"maximumCandidates": 1, "maximumDurationSeconds": 60},
                  "topK": 1
                }
                """.formatted(versionId), CommandDtos.StartExperimentRequest.class);
        SearchStartCommandFactory factory = mock(SearchStartCommandFactory.class);
        StartSearchExperimentUseCase.StartCommand expected =
                mock(StartSearchExperimentUseCase.StartCommand.class);
        when(factory.create(org.mockito.ArgumentMatchers.any())).thenReturn(expected);
        UUID owner = UUID.fromString("91000000-0000-4000-8000-000000000014");

        StartSearchExperimentUseCase.StartCommand mapped = new ExperimentRequestMapper(factory)
                .map(owner, "key", "hash", "correlation", request);

        ArgumentCaptor<SearchStartCommandFactory.Request> capture =
                ArgumentCaptor.forClass(SearchStartCommandFactory.Request.class);
        verify(factory).create(capture.capture());
        assertThat(mapped).isSameAs(expected);
        assertThat(capture.getValue().ownerUserId()).isEqualTo(owner);
        assertThat(capture.getValue().userStrategyVersionId().value()).isEqualTo(versionId);
        assertThat(capture.getValue().strategyId()).isNull();
        assertThat(capture.getValue().parameters()).isEmpty();
    }
}

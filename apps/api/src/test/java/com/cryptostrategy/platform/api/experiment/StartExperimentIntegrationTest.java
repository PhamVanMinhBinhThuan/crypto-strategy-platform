package com.cryptostrategy.platform.api.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.idempotency.IdempotencyCommandExecutor;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.StopExperimentUseCase;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

/** Public contract khi chỉ Start readiness switch được bật. */
@SuppressWarnings("unchecked")
class StartExperimentIntegrationTest {
    private static final UUID OWNER = UUID.fromString("91000000-0000-4000-8000-000000000001");
    private static final AuthenticatedUserContext USER =
            new AuthenticatedUserContext(OWNER, Instant.parse("2026-09-03T01:00:00Z"));

    @Test
    void enabledStartReturns202LocationAndDurableIdentities() {
        Fixture fixture = fixture();

        var response = fixture.controller.startExperiment(USER, "key-1", request());

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getHeaders().getLocation()).hasToString(
                "/api/v1/experiments/61000000000000000000000001");
        assertThat(response.getBody()).isEqualTo(new CommandDtos.ExperimentAcceptedResponse(
                new ExperimentId("61000000000000000000000001"),
                new JobId("61000000000000000000000002"), "QUEUED"));
        verify(fixture.mapper).map(org.mockito.ArgumentMatchers.eq(OWNER),
                org.mockito.ArgumentMatchers.eq("key-1"), org.mockito.ArgumentMatchers.eq("request-hash"),
                argThat(value -> value.length() == 26 && !value.contains("key-1")),
                org.mockito.ArgumentMatchers.eq(request()));
    }

    @Test
    void replayUsesTheSamePublishedOutcomeAndAuthenticatedOwner() {
        Fixture fixture = fixture();

        var first = fixture.controller.startExperiment(USER, "same-key", request());
        var replay = fixture.controller.startExperiment(USER, "same-key", request());

        assertThat(replay.getBody()).isEqualTo(first.getBody());
        verify(fixture.mapper, org.mockito.Mockito.times(2))
                .map(org.mockito.ArgumentMatchers.eq(OWNER), org.mockito.ArgumentMatchers.eq("same-key"),
                        org.mockito.ArgumentMatchers.eq("request-hash"),
                        argThat(value -> value.length() == 26 && !value.contains("same-key")),
                        org.mockito.ArgumentMatchers.eq(request()));

    }

    @Test
    void hostileIdempotencyKeyNeverBecomesCorrelationData() {
        Fixture fixture = fixture();
        String hostile = "secret-\"-\\-line\n\u0001";

        fixture.controller.startExperiment(USER, hostile, request());

        verify(fixture.mapper).map(org.mockito.ArgumentMatchers.eq(OWNER),
                org.mockito.ArgumentMatchers.eq(hostile), org.mockito.ArgumentMatchers.eq("request-hash"),
                argThat(value -> value.length() == 26 && !value.contains("secret")
                        && value.chars().noneMatch(Character::isISOControl)),
                org.mockito.ArgumentMatchers.eq(request()));
    }

    @Test
    void idempotencyConflictIsPreservedAtThePublicBoundary() {
        Fixture fixture = fixture();
        when(fixture.idempotency.execute(any(), anyString(), anyString(), any(), any(BiFunction.class)))
                .thenThrow(new IdempotencyConflictException("Idempotency conflict: payload does not match"));

        assertThatThrownBy(() -> fixture.controller.startExperiment(USER, "same-key", request()))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    private static Fixture fixture() {
        IdempotencyCommandExecutor idempotency = mock(IdempotencyCommandExecutor.class);
        StartSearchExperimentUseCase start = mock(StartSearchExperimentUseCase.class);
        ExperimentRequestMapper mapper = mock(ExperimentRequestMapper.class);
        var mapped = mock(StartSearchExperimentUseCase.StartCommand.class);
        when(mapper.map(any(), anyString(), anyString(), anyString(), any())).thenReturn(mapped);
        when(start.start(mapped)).thenReturn(new StartSearchExperimentUseCase.Acceptance(
                new ExperimentId("61000000000000000000000001"),
                new JobId("61000000000000000000000002"), "QUEUED", false));
        when(idempotency.execute(any(), anyString(), anyString(), any(), any(BiFunction.class)))
                .thenAnswer(call -> ((BiFunction<String, String, Object>) call.getArgument(4))
                        .apply(call.getArgument(2), "request-hash"));
        var controller = new ExperimentController(idempotency, mock(GetExperimentUseCase.class),
                mock(GetJobUseCase.class), mock(ListCandidatesUseCase.class),
                mock(StopExperimentUseCase.class), new PageRequestMapper(), start, mapper, true);
        return new Fixture(controller, mapper, idempotency);
    }

    private static CommandDtos.StartExperimentRequest request() {
        return new CommandDtos.StartExperimentRequest("F010 ready",
                new com.cryptostrategy.platform.domain.api.market.DatasetVersionId(
                        "61000000000000000000000004"),
                new CommandDtos.GeneratorSelectionRequest(
                        new CommandDtos.GeneratorId("random-search"), "1.0.0", 42L),
                null,
                new CommandDtos.SearchSpaceRequest(
                        new com.cryptostrategy.platform.strategy.api.model.StrategyPluginId("momentum"),
                        "1.0.0", java.util.Map.of("period",
                        new CommandDtos.ParameterRangeRequest(2L, 20L, null))),
                new CommandDtos.StopConditionRequest(10, 60), 3);
    }

    private record Fixture(ExperimentController controller, ExperimentRequestMapper mapper,
            IdempotencyCommandExecutor idempotency) {}
}

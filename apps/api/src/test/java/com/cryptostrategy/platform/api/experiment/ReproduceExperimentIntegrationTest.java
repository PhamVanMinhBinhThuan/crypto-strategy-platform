package com.cryptostrategy.platform.api.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.idempotency.IdempotencyCommandExecutor;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchReproductionUseCase;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.StopExperimentUseCase;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class ReproduceExperimentIntegrationTest {
    private static final AuthenticatedUserContext USER = new AuthenticatedUserContext(
            UUID.fromString("94000000-0000-4000-8000-000000000001"), Instant.parse("2026-09-03T03:00:00Z"));
    private static final String SOURCE = "64000000000000000000000001";

    @Test
    void acceptedAsyncReproductionReturns202LocationAndStableReplay() {
        Fixture fixture = fixture();
        var first = fixture.controller.reproduceExperiment(USER, "reproduce-key", SOURCE,
                new CommandDtos.ReproduceExperimentRequest("copy"));
        var replay = fixture.controller.reproduceExperiment(USER, "reproduce-key", SOURCE,
                new CommandDtos.ReproduceExperimentRequest("copy"));

        assertThat(first.getStatusCode().value()).isEqualTo(202);
        assertThat(first.getHeaders().getLocation()).hasToString(
                "/api/v1/experiments/64000000000000000000000002");
        assertThat(replay.getBody()).isEqualTo(first.getBody());
    }

    @Test
    void idempotencyConflictRemainsStable() {
        Fixture fixture = fixture();
        when(fixture.idempotency.execute(any(), anyString(), anyString(), any(), any(BiFunction.class)))
                .thenThrow(new IdempotencyConflictException("conflict"));
        assertThatThrownBy(() -> fixture.controller.reproduceExperiment(USER, "same-key", SOURCE,
                new CommandDtos.ReproduceExperimentRequest("different")))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    private static Fixture fixture() {
        IdempotencyCommandExecutor idempotency = mock(IdempotencyCommandExecutor.class);
        StartSearchReproductionUseCase reproduce = mock(StartSearchReproductionUseCase.class);
        when(reproduce.start(any())).thenReturn(new StartSearchReproductionUseCase.Acceptance(
                new ExperimentId("64000000000000000000000002"),
                new JobId("64000000000000000000000003"), "QUEUED", false));
        when(idempotency.execute(any(), anyString(), anyString(), any(), any(BiFunction.class)))
                .thenAnswer(call -> ((BiFunction<String, String, Object>) call.getArgument(4))
                        .apply(call.getArgument(2), "request-hash"));
        var controller = new ExperimentController(idempotency, mock(GetExperimentUseCase.class),
                mock(GetJobUseCase.class), mock(ListCandidatesUseCase.class),
                mock(StopExperimentUseCase.class), new PageRequestMapper(), null, null, true,
                reproduce, true);
        return new Fixture(controller, idempotency);
    }

    private record Fixture(ExperimentController controller, IdempotencyCommandExecutor idempotency) {}
}

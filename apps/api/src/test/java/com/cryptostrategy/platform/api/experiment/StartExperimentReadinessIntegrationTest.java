package com.cryptostrategy.platform.api.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.error.DependencyUnavailableException;
import com.cryptostrategy.platform.api.idempotency.IdempotencyCommandExecutor;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.StopExperimentUseCase;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Contract tiền kích hoạt: schema ổn định nhưng Start chưa được phép tạo durable graph. */
class StartExperimentReadinessIntegrationTest {
    private static final UUID OWNER_A = UUID.fromString("91000000-0000-4000-8000-000000000001");
    private static final UUID OWNER_B = UUID.fromString("91000000-0000-4000-8000-000000000002");
    private static final AuthenticatedUserContext USER_A =
            new AuthenticatedUserContext(OWNER_A, Instant.parse("2026-09-03T01:00:00Z"));
    private static final AuthenticatedUserContext USER_B =
            new AuthenticatedUserContext(OWNER_B, Instant.parse("2026-09-03T01:00:00Z"));

    @Test
    void acceptedResponseKeepsThePublishedDurableIdentityContract() {
        var response = new CommandDtos.ExperimentAcceptedResponse(
                new ExperimentId("61000000000000000000000001"),
                new JobId("61000000000000000000000002"),
                "QUEUED");

        assertThat(response.experimentId().value()).isEqualTo("61000000000000000000000001");
        assertThat(response.jobId().value()).isEqualTo("61000000000000000000000002");
        assertThat(response.status()).isEqualTo("QUEUED");
    }

    @Test
    void exactReplayRemainsReadinessGatedAndCannotCreateMoreThanOneLogicalOutcome() {
        Fixture fixture = fixture();

        for (int replay = 0; replay < 100; replay++) {
            assertThatThrownBy(() -> fixture.controller.startExperiment(
                            USER_A, "same-idempotency-key", request()))
                    .isInstanceOf(DependencyUnavailableException.class)
                    .hasMessage("Search Coordinator");
        }

        verifyNoInteractions(
                fixture.idempotency,
                fixture.experiments,
                fixture.jobs,
                fixture.candidates,
                fixture.stopExperiment);
    }

    @Test
    void foreignAndMissingOwnershipPathsAreIndistinguishableWhileGateIsClosed() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.controller.startExperiment(USER_A, "owner-a", request()))
                .isInstanceOf(DependencyUnavailableException.class)
                .hasMessage("Search Coordinator");
        assertThatThrownBy(() -> fixture.controller.startExperiment(USER_B, "owner-b", request()))
                .isInstanceOf(DependencyUnavailableException.class)
                .hasMessage("Search Coordinator");

        verifyNoInteractions(fixture.experiments);
    }

    private static CommandDtos.StartExperimentRequest request() {
        return new CommandDtos.StartExperimentRequest(
                "F010 readiness",
                new com.cryptostrategy.platform.domain.api.market.DatasetVersionId(
                        "61000000000000000000000004"),
                new CommandDtos.GeneratorSelectionRequest(
                        new CommandDtos.GeneratorId("random-search"), "1.0.0", 42L),
                new CommandDtos.SearchSpaceRequest(
                        new com.cryptostrategy.platform.strategy.api.model.StrategyPluginId("momentum"),
                        "1.0.0",
                        java.util.Map.of(
                                "period", new CommandDtos.ParameterRangeRequest(2L, 20L, null))),
                new CommandDtos.StopConditionRequest(10, 60),
                3);
    }

    private static Fixture fixture() {
        IdempotencyCommandExecutor idempotency = mock(IdempotencyCommandExecutor.class);
        GetExperimentUseCase experiments = mock(GetExperimentUseCase.class);
        GetJobUseCase jobs = mock(GetJobUseCase.class);
        ListCandidatesUseCase candidates = mock(ListCandidatesUseCase.class);
        StopExperimentUseCase stopExperiment = mock(StopExperimentUseCase.class);
        ExperimentController controller = new ExperimentController(
                idempotency,
                experiments,
                jobs,
                candidates,
                stopExperiment,
                new PageRequestMapper());
        return new Fixture(controller, idempotency, experiments, jobs, candidates, stopExperiment);
    }

    private record Fixture(
            ExperimentController controller,
            IdempotencyCommandExecutor idempotency,
            GetExperimentUseCase experiments,
            GetJobUseCase jobs,
            ListCandidatesUseCase candidates,
            StopExperimentUseCase stopExperiment) {}
}

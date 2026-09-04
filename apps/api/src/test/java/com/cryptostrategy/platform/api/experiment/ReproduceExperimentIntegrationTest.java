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
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.Money;
import com.cryptostrategy.platform.backtesting.api.model.Quantity;
import com.cryptostrategy.platform.backtesting.api.model.Trade;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.execution.api.ExecutionEvidence;
import com.cryptostrategy.platform.execution.api.ReproductionVerificationId;
import com.cryptostrategy.platform.execution.api.port.in.GetSearchReproductionVerificationUseCase;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchReproductionUseCase;
import com.cryptostrategy.platform.execution.api.port.out.SearchReproductionGateway;
import com.cryptostrategy.platform.execution.api.port.out.SearchReproductionVerificationGateway;
import com.cryptostrategy.platform.execution.internal.SearchReproductionApplicationService;
import com.cryptostrategy.platform.execution.internal.SearchReproductionVerificationCoordinator;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.StopExperimentUseCase;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevision;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    void emptyJsonBodyUsesTheDefaultReproductionName() {
        Fixture fixture = fixture();

        fixture.controller.reproduceExperiment(USER, "reproduce-key", SOURCE,
                new CommandDtos.ReproduceExperimentRequest(null));

        ArgumentCaptor<StartSearchReproductionUseCase.Command> command =
                ArgumentCaptor.forClass(StartSearchReproductionUseCase.Command.class);
        org.mockito.Mockito.verify(fixture.reproduce).start(command.capture());
        assertThat(command.getValue().name()).isEqualTo("Reproduction of " + SOURCE);
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

    @Test
    void usesFixedUtcClockAndDoesNotLeakHostileIdempotencyKeyIntoCorrelation() {
        Fixture fixture = fixture();
        String hostile = "secret-\"-\\-line\n\u0001";

        fixture.controller.reproduceExperiment(USER, hostile, SOURCE,
                new CommandDtos.ReproduceExperimentRequest("copy"));

        ArgumentCaptor<StartSearchReproductionUseCase.Command> command =
                ArgumentCaptor.forClass(StartSearchReproductionUseCase.Command.class);
        org.mockito.Mockito.verify(fixture.reproduce).start(command.capture());
        assertThat(command.getValue().requestedAt()).isEqualTo(Instant.parse("2026-09-03T03:30:00Z"));
        assertThat(command.getValue().correlationId()).hasSize(26).doesNotContain("secret");
    }

    @Test
    void reproductionCreatesLinkedArtifactsWithoutMutatingSourceAndPublishesMatchedVerdict() {
        RecordingReproductionGateway gateway = new RecordingReproductionGateway();
        FrozenGraph sourceBefore = gateway.source;
        IdempotencyCommandExecutor idempotency = replayingIdempotency();
        var controller = controller(idempotency, new SearchReproductionApplicationService(gateway));

        var response = controller.reproduceExperiment(USER, "immutable-source-key", SOURCE,
                new CommandDtos.ReproduceExperimentRequest("independent reproduction"));

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(gateway.source).isEqualTo(sourceBefore);
        assertThat(gateway.created.sourceExperimentId()).isEqualTo(new ExperimentId(SOURCE));
        assertThat(gateway.created.experimentId()).isNotEqualTo(gateway.created.sourceExperimentId());
        assertThat(gateway.created.candidates()).hasSize(1).allSatisfy(copy -> {
            assertThat(copy.sourceCandidateId().value()).isEqualTo(sourceBefore.candidate().id());
            assertThat(copy.candidateId().value()).isNotEqualTo(sourceBefore.candidate().id());
        });

        FrozenGraph reproduced = gateway.completeTargetEvidence();
        assertCanonicalCopyHasFreshRuntimeIdentity(sourceBefore, reproduced);
        assertThat(gateway.source).isEqualTo(sourceBefore);

        RecordingVerificationGateway verification = new RecordingVerificationGateway(
                gateway.created.verificationId(), USER.userId(), gateway.created.sourceExperimentId(),
                gateway.created.experimentId());
        ExecutionEvidence sourceEvidence = matchingEvidence();
        ExecutionEvidence reproducedEvidence = matchingEvidence();
        var coordinator = new SearchReproductionVerificationCoordinator(
                verification,
                (owner, experimentId) -> experimentId.equals(gateway.created.sourceExperimentId())
                        ? sourceEvidence
                        : reproducedEvidence,
                Clock.fixed(Instant.parse("2026-09-03T03:31:00Z"), ZoneOffset.UTC));

        assertThat(coordinator.verify(gateway.created.experimentId()))
                .isEqualTo(SearchReproductionVerificationCoordinator.Result.MATCHED);
        assertThat(verification.completion.status()).isEqualTo("MATCHED");
        assertThat(verification.completion.tradesMatched()).isTrue();
        assertThat(verification.completion.metricsMatched()).isTrue();
        assertThat(verification.completion.fingerprintsMatched()).isTrue();
        assertThat(gateway.source).isEqualTo(sourceBefore);
    }

    @Test
    void ownerCanReadTheDurableReproductionVerdictWithoutLeakingAnotherUsersRecord() {
        var snapshot = new GetSearchReproductionVerificationUseCase.Snapshot(
                new ReproductionVerificationId("65000000000000000000000003"),
                new ExperimentId(SOURCE),
                new ExperimentId("64000000000000000000000002"),
                "MATCHED", true, true, true,
                "sha256:source", "sha256:reproduction", Map.of(), null, null,
                Instant.parse("2026-09-03T03:30:30Z"),
                Instant.parse("2026-09-03T03:31:00Z"),
                Instant.parse("2026-09-03T03:31:00Z"));
        GetSearchReproductionVerificationUseCase query = (owner, id) ->
                USER.userId().equals(owner) && snapshot.reproductionExperimentId().equals(id)
                        ? Optional.of(snapshot)
                        : Optional.empty();
        var controller = new ReproductionVerificationController(query);

        var response = controller.get(USER, snapshot.reproductionExperimentId().value());

        assertThat(response.status()).isEqualTo("MATCHED");
        assertThat(response.tradesMatched()).isTrue();
        assertThat(response.metricsMatched()).isTrue();
        assertThat(response.fingerprintsMatched()).isTrue();
        assertThat(response.sourceExperimentId()).isEqualTo(new ExperimentId(SOURCE));
        assertThatThrownBy(() -> controller.get(
                        new AuthenticatedUserContext(UUID.randomUUID(), USER.authenticationExpiresAt()),
                        snapshot.reproductionExperimentId().value()))
                .isInstanceOf(com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException.class);
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
        return new Fixture(controller(idempotency, reproduce), idempotency, reproduce);
    }

    private static IdempotencyCommandExecutor replayingIdempotency() {
        IdempotencyCommandExecutor idempotency = mock(IdempotencyCommandExecutor.class);
        when(idempotency.execute(any(), anyString(), anyString(), any(), any(BiFunction.class)))
                .thenAnswer(call -> ((BiFunction<String, String, Object>) call.getArgument(4))
                        .apply(call.getArgument(2), "request-hash"));
        return idempotency;
    }

    private static ExperimentController controller(IdempotencyCommandExecutor idempotency,
            StartSearchReproductionUseCase reproduce) {
        return new ExperimentController(idempotency, mock(GetExperimentUseCase.class),
                mock(GetJobUseCase.class), mock(ListCandidatesUseCase.class),
                mock(StopExperimentUseCase.class), new PageRequestMapper(), null, null, true,
                reproduce, true, Clock.fixed(Instant.parse("2026-09-03T03:30:00Z"), ZoneOffset.UTC));
    }

    private static void assertCanonicalCopyHasFreshRuntimeIdentity(FrozenGraph source, FrozenGraph reproduced) {
        assertThat(reproduced.experimentId()).isNotEqualTo(source.experimentId());
        assertThat(reproduced.manifest()).isEqualTo(source.manifest());
        assertThat(reproduced.candidate().id()).isNotEqualTo(source.candidate().id());
        assertThat(reproduced.candidate().fingerprint()).isEqualTo(source.candidate().fingerprint());
        assertThat(reproduced.acceptedResult().id()).isNotEqualTo(source.acceptedResult().id());
        assertThat(reproduced.acceptedResult().fingerprint())
                .isEqualTo(source.acceptedResult().fingerprint());
        assertThat(reproduced.trades()).zipSatisfy(source.trades(), (target, original) -> {
            assertThat(target.id()).isNotEqualTo(original.id());
            assertThat(target.fingerprint()).isEqualTo(original.fingerprint());
        });
        assertThat(reproduced.evaluation().id()).isNotEqualTo(source.evaluation().id());
        assertThat(reproduced.evaluation().fingerprint()).isEqualTo(source.evaluation().fingerprint());
        assertThat(reproduced.leaderboardRevision().id()).isNotEqualTo(source.leaderboardRevision().id());
        assertThat(reproduced.leaderboardRevision().fingerprint())
                .isEqualTo(source.leaderboardRevision().fingerprint());
    }

    private static ExecutionEvidence matchingEvidence() {
        Trade trade = mock(Trade.class);
        when(trade.sequence()).thenReturn(0);
        when(trade.entryTime()).thenReturn(Instant.parse("2026-09-01T00:00:00Z"));
        when(trade.exitTime()).thenReturn(Instant.parse("2026-09-01T01:00:00Z"));
        when(trade.entryPrice()).thenReturn(new Money(new BigDecimal("100")));
        when(trade.exitPrice()).thenReturn(new Money(new BigDecimal("110")));
        when(trade.quantity()).thenReturn(new Quantity(BigDecimal.ONE));
        when(trade.totalFee()).thenReturn(new Money(new BigDecimal("0.2")));
        when(trade.realizedPnl()).thenReturn(new BigDecimal("9.8"));

        BacktestResult backtest = mock(BacktestResult.class);
        when(backtest.trades()).thenReturn(List.of(trade));
        when(backtest.fingerprint()).thenReturn("sha256:accepted-result");
        EvaluationResult evaluation = mock(EvaluationResult.class);
        when(evaluation.totalReturn()).thenReturn(new BigDecimal("0.10"));
        when(evaluation.winRate()).thenReturn(new BigDecimal("1.00"));
        when(evaluation.maximumDrawdown()).thenReturn(new BigDecimal("0.02"));
        when(evaluation.numberOfTrades()).thenReturn(1);
        when(evaluation.fingerprint()).thenReturn("sha256:evaluation");
        LeaderboardRevision leaderboard = mock(LeaderboardRevision.class);
        when(leaderboard.fingerprint()).thenReturn("sha256:leaderboard-revision");
        return new ExecutionEvidence(backtest, evaluation, leaderboard);
    }

    private static final class RecordingReproductionGateway implements SearchReproductionGateway {
        private final FrozenGraph source = FrozenGraph.source();
        private CreateCommand created;

        @Override
        public Optional<SourceSnapshot> loadSource(UUID ownerUserId, ExperimentId sourceExperimentId) {
            if (!USER.userId().equals(ownerUserId) || !new ExperimentId(SOURCE).equals(sourceExperimentId)) {
                return Optional.empty();
            }
            return Optional.of(new SourceSnapshot(sourceExperimentId, "COMPLETED", true,
                    List.of(source.candidate().id())));
        }

        @Override
        public Result create(CreateCommand command) {
            created = command;
            return new Result(Result.Status.CREATED, command.experimentId(), command.searchJobId());
        }

        FrozenGraph completeTargetEvidence() {
            if (created == null) throw new IllegalStateException("Reproduction was not created");
            return source.reproducedAs(created.experimentId().value(),
                    created.candidates().getFirst().candidateId().value());
        }
    }

    private static final class RecordingVerificationGateway
            implements SearchReproductionVerificationGateway {
        private final Work work;
        private Completion completion;

        private RecordingVerificationGateway(ReproductionVerificationId verificationId, UUID owner,
                ExperimentId source, ExperimentId reproduction) {
            work = new Work(verificationId, 0, owner, source, reproduction);
        }

        @Override
        public List<ExperimentId> findReady(int limit) {
            return List.of(work.reproductionExperimentId());
        }

        @Override
        public Optional<Work> claimReady(ExperimentId reproductionExperimentId, Instant now) {
            return completion == null && work.reproductionExperimentId().equals(reproductionExperimentId)
                    ? Optional.of(work)
                    : Optional.empty();
        }

        @Override
        public boolean complete(Completion completed) {
            completion = completed;
            return true;
        }
    }

    private record FrozenArtifact(String id, String fingerprint) {}

    private record FrozenGraph(String experimentId, String manifest, FrozenArtifact candidate,
            FrozenArtifact acceptedResult, List<FrozenArtifact> trades, FrozenArtifact evaluation,
            FrozenArtifact leaderboardRevision) {
        private FrozenGraph {
            trades = List.copyOf(trades);
        }

        static FrozenGraph source() {
            return new FrozenGraph(SOURCE, "sha256:manifest",
                    new FrozenArtifact("64000000000000000000000011", "sha256:candidate"),
                    new FrozenArtifact("64000000000000000000000012", "sha256:accepted-result"),
                    List.of(new FrozenArtifact("64000000000000000000000013", "sha256:trade-0")),
                    new FrozenArtifact("64000000000000000000000014", "sha256:evaluation"),
                    new FrozenArtifact("64000000000000000000000015", "sha256:leaderboard-revision"));
        }

        FrozenGraph reproducedAs(String targetExperimentId, String targetCandidateId) {
            return new FrozenGraph(targetExperimentId, manifest,
                    new FrozenArtifact(targetCandidateId, candidate.fingerprint()),
                    new FrozenArtifact("65000000000000000000000012", acceptedResult.fingerprint()),
                    List.of(new FrozenArtifact("65000000000000000000000013",
                            trades.getFirst().fingerprint())),
                    new FrozenArtifact("65000000000000000000000014", evaluation.fingerprint()),
                    new FrozenArtifact("65000000000000000000000015",
                            leaderboardRevision.fingerprint()));
        }
    }

    private record Fixture(ExperimentController controller, IdempotencyCommandExecutor idempotency,
            StartSearchReproductionUseCase reproduce) {}
}

package com.cryptostrategy.platform.execution.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase;
import com.cryptostrategy.platform.execution.api.port.out.TrustedSearchCoordinationGateway;
import com.cryptostrategy.platform.search.api.model.GeneratorDescriptor;
import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import com.cryptostrategy.platform.search.api.model.SearchRunMode;
import com.cryptostrategy.platform.search.api.model.SearchRunStatus;
import com.cryptostrategy.platform.search.api.model.SearchStopConditions;
import com.cryptostrategy.platform.search.api.model.SearchTerminalReason;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TrustedSearchCoordinationServiceTest {
    private static final Instant START = Instant.parse("2026-09-03T00:00:00Z");

    @Test
    void authoritativeOnTimeCompletionWinsAndCompletesExactlyAtItsTimestamp() {
        TrustedSearchCoordinationGateway gateway = mock(TrustedSearchCoordinationGateway.class);
        SearchRun running = pending().start(START);
        Instant completedAt = START.plusSeconds(30);
        var before = snapshot(running, 2, 2, 0, completedAt);
        var after = snapshot(running.complete(completedAt), 2, 2, 0, completedAt);
        when(gateway.loadCompletion(experiment(running), candidate(), job())).thenReturn(Optional.of(before));
        when(gateway.commit(any())).thenReturn(true);
        when(gateway.load(experiment(running))).thenReturn(Optional.of(after));
        var service = new TrustedSearchCoordinationService(gateway, Clock.fixed(START, ZoneOffset.UTC));

        var result = service.reconcileCompletion(new TrustedSearchCoordinationUseCase.CompletionTrigger(
                "message", experiment(running), candidate(), job(), START.plusSeconds(40), "correlation"));

        assertThat(result.status()).isEqualTo(SearchRunStatus.COMPLETED);
        assertThat(result.decision()).isEqualTo(TrustedSearchCoordinationUseCase.Decision.COMPLETE);
        verify(gateway).commit(new TrustedSearchCoordinationGateway.Transition(
                running.version(), after.run(), 2, 0, "message"));
    }

    @Test
    void completionNeverMovesDurableTimeBackwardsWhenTheAuthoritativeEventIsOlder() {
        TrustedSearchCoordinationGateway gateway = mock(TrustedSearchCoordinationGateway.class);
        SearchRun started = pending().start(START);
        Instant advancedAt = START.plusSeconds(20);
        SearchRun running = started.advance(
                new GeneratorState("random-state-v1", "{\"cursor\":1}", "advanced-state"),
                1,
                advancedAt);
        Instant completedAt = START.plusSeconds(10);
        SearchRun completed = running.complete(advancedAt);
        var before = snapshot(running, 2, 2, 0, completedAt);
        var after = snapshot(completed, 2, 2, 0, completedAt);
        when(gateway.loadCompletion(experiment(running), candidate(), job())).thenReturn(Optional.of(before));
        when(gateway.commit(any())).thenReturn(true);
        when(gateway.load(experiment(running))).thenReturn(Optional.of(after));
        var service = new TrustedSearchCoordinationService(gateway, Clock.fixed(advancedAt, ZoneOffset.UTC));

        var result = service.reconcileCompletion(new TrustedSearchCoordinationUseCase.CompletionTrigger(
                "older-message", experiment(running), candidate(), job(), completedAt, "correlation"));

        assertThat(result.status()).isEqualTo(SearchRunStatus.COMPLETED);
        verify(gateway).commit(new TrustedSearchCoordinationGateway.Transition(
                running.version(), completed, 2, 0, "older-message"));
    }

    @Test
    void lateCompletionCannotBeatFrozenDeadlineAndStopsAfterAllChildrenSettle() {
        TrustedSearchCoordinationGateway gateway = mock(TrustedSearchCoordinationGateway.class);
        SearchRun running = pending().start(START);
        Instant late = running.deadlineAt().plusSeconds(1);
        SearchRun stopping = running.requestStop(late);
        SearchRun stopped = stopping.stop(late);
        var before = snapshot(running, 2, 2, 0, late);
        var afterStopping = snapshot(stopping, 2, 2, 0, late);
        var afterStopped = snapshot(stopped, 2, 2, 0, late);
        when(gateway.loadCompletion(experiment(running), candidate(), job())).thenReturn(Optional.of(before));
        when(gateway.commit(any())).thenReturn(true);
        when(gateway.load(experiment(running)))
                .thenReturn(Optional.of(afterStopping))
                .thenReturn(Optional.of(afterStopped));
        var service = new TrustedSearchCoordinationService(gateway, Clock.fixed(START, ZoneOffset.UTC));

        var result = service.reconcileCompletion(new TrustedSearchCoordinationUseCase.CompletionTrigger(
                "message", experiment(running), candidate(), job(), late, "correlation"));

        assertThat(result.status()).isEqualTo(SearchRunStatus.STOPPED);
        assertThat(result.decision()).isEqualTo(TrustedSearchCoordinationUseCase.Decision.STOP);
        assertThat(running.deadlineAt()).isEqualTo(START.plusSeconds(60));
    }

    @Test
    void deterministicSettledPrefixStopsAfterConfiguredNoImprovementThreshold() {
        TrustedSearchCoordinationGateway gateway = mock(TrustedSearchCoordinationGateway.class);
        SearchRun running = pending(10, 2).start(START);
        Instant completedAt = START.plusSeconds(20);
        SearchRun stopping = running.requestStop(completedAt, SearchTerminalReason.NO_IMPROVEMENT);
        SearchRun stopped = stopping.stop(completedAt);
        var before = new TrustedSearchCoordinationGateway.AuthoritativeSnapshot(
                running, 3, 3, 0, completedAt, 10, 2);
        var afterStopping = new TrustedSearchCoordinationGateway.AuthoritativeSnapshot(
                stopping, 3, 3, 0, completedAt, 10, 2);
        var afterStopped = new TrustedSearchCoordinationGateway.AuthoritativeSnapshot(
                stopped, 3, 3, 0, completedAt, 10, 2);
        when(gateway.loadCompletion(experiment(running), candidate(), job()))
                .thenReturn(Optional.of(before));
        when(gateway.commit(any())).thenReturn(true);
        when(gateway.load(experiment(running)))
                .thenReturn(Optional.of(afterStopping))
                .thenReturn(Optional.of(afterStopped));

        var result = new TrustedSearchCoordinationService(
                gateway, Clock.fixed(completedAt, ZoneOffset.UTC)).reconcileCompletion(
                        new TrustedSearchCoordinationUseCase.CompletionTrigger(
                                "no-improvement", experiment(running), candidate(), job(),
                                completedAt, "correlation"));

        assertThat(result.status()).isEqualTo(SearchRunStatus.STOPPED);
        assertThat(afterStopped.run().terminalReason()).isEqualTo(SearchTerminalReason.NO_IMPROVEMENT);
    }

    private static TrustedSearchCoordinationGateway.AuthoritativeSnapshot snapshot(
            SearchRun run, int allocated, int completed, int failed, Instant latest) {
        return new TrustedSearchCoordinationGateway.AuthoritativeSnapshot(
                run, allocated, completed, failed, latest);
    }

    private static SearchRun pending() {
        return pending(2, null);
    }

    private static SearchRun pending(int maximumCandidates, Integer maximumWithoutImprovement) {
        return SearchRun.pending(new SearchRunId("01J7K8M9N0P1Q2R3S4T5A6V7W1"),
                new com.cryptostrategy.platform.search.api.model.SearchExperimentId("01J7K8M9N0P1Q2R3S4T5A6V7W2"),
                new com.cryptostrategy.platform.search.api.model.SearchJobId("01J7K8M9N0P1Q2R3S4T5A6V7W3"),
                SearchRunMode.GENERATION, null,
                new GeneratorDescriptor(new GeneratorId("random-search"), GeneratorVersion.parse("1.0.0"),
                        "random-state-v1", Set.of(ParameterType.INTEGER), "descriptor"),
                7, "space", new GeneratorState("random-state-v1", "{}", "state"),
                new SearchStopConditions(maximumCandidates, Duration.ofSeconds(60),
                        maximumWithoutImprovement), 1, START.minusSeconds(1));
    }

    private static com.cryptostrategy.platform.experiment.api.ExperimentId experiment(SearchRun run) {
        return new com.cryptostrategy.platform.experiment.api.ExperimentId(run.experimentId().value());
    }

    private static com.cryptostrategy.platform.experiment.api.CandidateId candidate() {
        return new com.cryptostrategy.platform.experiment.api.CandidateId("01J7K8M9N0P1Q2R3S4T5A6V7W2");
    }

    private static com.cryptostrategy.platform.experiment.api.job.JobId job() {
        return new com.cryptostrategy.platform.experiment.api.job.JobId("01J7K8M9N0P1Q2R3S4T5A6V7W3");
    }
}

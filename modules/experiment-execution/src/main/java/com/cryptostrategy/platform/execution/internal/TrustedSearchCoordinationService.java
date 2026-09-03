package com.cryptostrategy.platform.execution.internal;

import com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase;
import com.cryptostrategy.platform.execution.api.port.out.TrustedSearchCoordinationGateway;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunStatus;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Quyết định lifecycle deterministic từ durable truth; không tin counter/timestamp trong message. */
public final class TrustedSearchCoordinationService implements TrustedSearchCoordinationUseCase {
    private final TrustedSearchCoordinationGateway gateway;
    private final Clock clock;

    public TrustedSearchCoordinationService(TrustedSearchCoordinationGateway gateway, Clock clock) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public CoordinationOutcome reconcileCompletion(CompletionTrigger trigger) {
        var snapshot = gateway.loadCompletion(
                        trigger.experimentId().value(), trigger.candidateId().value(), trigger.backtestJobId().value())
                .orElseThrow(() -> new IllegalArgumentException("Search completion is inaccessible"));
        return decideAndCommit(snapshot, trigger.observedAt(), trigger.messageId());
    }

    @Override
    public CoordinationOutcome reconcileRun(ReconciliationTrigger trigger) {
        var snapshot = gateway.load(trigger.experimentId().value())
                .orElseThrow(() -> new IllegalArgumentException("Search Run is inaccessible"));
        return decideAndCommit(snapshot, trigger.observedAt(), null);
    }

    @Override
    public CoordinationOutcome requestStop(StopTrigger trigger) {
        var snapshot = gateway.load(trigger.experimentId().value())
                .orElseThrow(() -> new IllegalArgumentException("Search Run is inaccessible"));
        SearchRun run = snapshot.run();
        if (run.status().isTerminal()) return outcome(snapshot, Decision.ALREADY_TERMINAL);
        SearchRun replacement = run.status() == SearchRunStatus.STOPPING
                ? run
                : run.requestStop(trigger.requestedAt());
        if (replacement == run) return outcome(snapshot, Decision.WAIT_FOR_COMPLETIONS);
        commit(snapshot, replacement, null);
        return outcome(reload(run.experimentRef()), Decision.WAIT_FOR_COMPLETIONS);
    }

    private CoordinationOutcome decideAndCommit(
            TrustedSearchCoordinationGateway.AuthoritativeSnapshot snapshot,
            Instant observedAt,
            String messageId) {
        SearchRun run = snapshot.run();
        if (run.status().isTerminal()) return outcome(snapshot, Decision.ALREADY_TERMINAL);
        if (run.status() == SearchRunStatus.PENDING) {
            commit(snapshot, run.start(clock.instant()), messageId);
            snapshot = reload(run.experimentRef());
            run = snapshot.run();
        }
        boolean deadlineReached = !observedAt.isBefore(run.deadlineAt());
        boolean allSettled = snapshot.activeWork() == 0;
        boolean generationFinished = snapshot.allocatedWork() >= run.stopConditions().maximumCandidates();
        boolean completionWasOnTime = snapshot.latestAuthoritativeCompletedAt() == null
                || !snapshot.latestAuthoritativeCompletedAt().isAfter(run.deadlineAt());

        if (run.status() == SearchRunStatus.STOPPING) {
            if (!allSettled) return outcome(snapshot, Decision.WAIT_FOR_COMPLETIONS);
            commit(snapshot, run.stop(observedAt), messageId);
            return outcome(reload(run.experimentRef()), Decision.STOP);
        }
        if (generationFinished && allSettled && completionWasOnTime) {
            commit(snapshot, run.complete(snapshot.latestAuthoritativeCompletedAt() == null
                    ? observedAt : snapshot.latestAuthoritativeCompletedAt()), messageId);
            return outcome(reload(run.experimentRef()), Decision.COMPLETE);
        }
        if (deadlineReached) {
            SearchRun stopping = run.requestStop(observedAt);
            commit(snapshot, stopping, messageId);
            var reloaded = reload(run.experimentRef());
            if (reloaded.activeWork() == 0) {
                // Message receipt đã được claim bởi transition RUNNING -> STOPPING ngay phía trên.
                commit(reloaded, reloaded.run().stop(observedAt), null);
                return outcome(reload(run.experimentRef()), Decision.STOP);
            }
            return outcome(reloaded, Decision.WAIT_FOR_COMPLETIONS);
        }
        return outcome(snapshot, generationFinished ? Decision.WAIT_FOR_COMPLETIONS : Decision.FILL_AVAILABLE_SLOTS);
    }

    private void commit(TrustedSearchCoordinationGateway.AuthoritativeSnapshot snapshot,
                        SearchRun replacement, String messageId) {
        if (!gateway.commit(new TrustedSearchCoordinationGateway.Transition(
                snapshot.run().version(), replacement, snapshot.completedWork(),
                snapshot.failedWork(), messageId))) {
            throw new IllegalStateException("Search coordination fence changed");
        }
    }

    private TrustedSearchCoordinationGateway.AuthoritativeSnapshot reload(String experimentId) {
        return gateway.load(experimentId).orElseThrow(() -> new IllegalStateException("Search Run disappeared"));
    }

    private static CoordinationOutcome outcome(
            TrustedSearchCoordinationGateway.AuthoritativeSnapshot snapshot, Decision decision) {
        return new CoordinationOutcome(snapshot.run().searchRunId(), snapshot.allocatedWork(),
                snapshot.completedWork(), snapshot.failedWork(), snapshot.run().status(), decision);
    }
}

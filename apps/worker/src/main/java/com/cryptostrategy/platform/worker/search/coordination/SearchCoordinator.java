package com.cryptostrategy.platform.worker.search.coordination;

import com.cryptostrategy.platform.contracts.api.SearchRequestPayload;
import com.cryptostrategy.platform.execution.api.port.in.SearchCandidateAllocationUseCase;
import com.cryptostrategy.platform.execution.api.port.in.SearchCoordinationCommand;
import com.cryptostrategy.platform.execution.api.port.in.SearchCoordinationResult;
import com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.SearchModuleFactory;
import java.util.Objects;
import java.time.Instant;
import java.time.Clock;

/** Adapter Worker mỏng; durable application port quyết định allocation và progress. */
public class SearchCoordinator {
    private final SearchCandidateAllocationUseCase allocations;
    private final WorkerProperties properties;
    private final SearchRunStore runs;
    private final TrustedSearchCoordinationUseCase trusted;
    private final SearchModuleFactory.Components search;
    private final Clock clock;

    public SearchCoordinator(
            SearchCandidateAllocationUseCase allocations,
            WorkerProperties properties,
            SearchRunStore runs,
            TrustedSearchCoordinationUseCase trusted,
            SearchModuleFactory.Components search,
            Clock clock) {
        this.allocations = Objects.requireNonNull(allocations, "allocations");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.runs = Objects.requireNonNull(runs, "runs");
        this.trusted = Objects.requireNonNull(trusted, "trusted");
        this.search = Objects.requireNonNull(search, "search");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SearchCoordinationResult coordinate(SearchRequestPayload request, String correlationId) {
        Objects.requireNonNull(request, "request");
        SearchRun durable = runs.findBySearchJobId(new com.cryptostrategy.platform.search.api.model.SearchJobId(request.searchJobId().value()))
                    .orElseThrow(() -> new IllegalArgumentException("Search Run is inaccessible"));
            if (!durable.experimentId().value().equals(request.experimentId().value())) {
                throw new IllegalArgumentException("Search Run is inaccessible");
            }
            search.requireGenerator(durable.generatorId(), durable.generatorVersion());
            if (durable.status().isTerminal()) {
                return new SearchCoordinationResult(
                        durable.searchRunId(), (int) durable.nextGenerationIndex(), 0, 0, 0,
                        durable.status());
            }
            var lifecycle = trusted.reconcileRun(new TrustedSearchCoordinationUseCase.ReconciliationTrigger(
                    new com.cryptostrategy.platform.experiment.api.ExperimentId(durable.experimentId().value()), clock.instant(), correlationId));
            if (durable.mode() == com.cryptostrategy.platform.search.api.model.SearchRunMode.REPRODUCTION) {
                return new SearchCoordinationResult(lifecycle.searchRunId(), lifecycle.allocatedWork(),
                        lifecycle.allocatedWork() - lifecycle.completedWork() - lifecycle.failedWork(),
                        lifecycle.completedWork(), lifecycle.failedWork(), lifecycle.status());
            }
            if (lifecycle.decision() != TrustedSearchCoordinationUseCase.Decision.FILL_AVAILABLE_SLOTS) {
                return new SearchCoordinationResult(lifecycle.searchRunId(), lifecycle.allocatedWork(),
                        lifecycle.allocatedWork() - lifecycle.completedWork() - lifecycle.failedWork(),
                        lifecycle.completedWork(), lifecycle.failedWork(), lifecycle.status());
            }
        int boundedWindow = Math.min(
                request.concurrencyHint(),
                properties.concurrency().maxInFlightPerExperiment());
        return allocations.fillAvailableSlots(new SearchCoordinationCommand(
                new com.cryptostrategy.platform.experiment.api.job.JobId(request.searchJobId().value()),
                new com.cryptostrategy.platform.experiment.api.ExperimentId(request.experimentId().value()),
                boundedWindow,
                properties.concurrency().maxInFlight(),
                request.topKTarget(),
                correlationId));
    }

    public TrustedSearchCoordinationUseCase.CoordinationOutcome complete(
            TrustedSearchCoordinationUseCase.CompletionTrigger trigger) {
        SearchRun run = requireDurableRun(trigger.experimentId().value());
        var outcome = requireTrusted().reconcileCompletion(trigger);
        refillWhenRequested(run, outcome, trigger.correlationId());
        return outcome;
    }

    public TrustedSearchCoordinationUseCase.CoordinationOutcome reconcile(
            String experimentId, Instant observedAt, String correlationId) {
        SearchRun run = requireDurableRun(experimentId);
        var outcome = requireTrusted().reconcileRun(
                new TrustedSearchCoordinationUseCase.ReconciliationTrigger(
                        new com.cryptostrategy.platform.experiment.api.ExperimentId(experimentId),
                        observedAt,
                        correlationId));
        refillWhenRequested(run, outcome, correlationId);
        return outcome;
    }

    public TrustedSearchCoordinationUseCase.CoordinationOutcome stop(
            String experimentId, Instant requestedAt, String correlationId) {
        requireDurableRun(experimentId);
        return requireTrusted().requestStop(
                new TrustedSearchCoordinationUseCase.StopTrigger(new com.cryptostrategy.platform.experiment.api.ExperimentId(experimentId), requestedAt, correlationId));
    }

    private SearchRun requireDurableRun(String experimentId) {
        return runs.findByExperimentId(new com.cryptostrategy.platform.search.api.model.SearchExperimentId(experimentId))
                .orElseThrow(() -> new IllegalArgumentException("Search Run is inaccessible"));
    }

    private TrustedSearchCoordinationUseCase requireTrusted() {
        return trusted;
    }

    private void refillWhenRequested(
            SearchRun run,
            TrustedSearchCoordinationUseCase.CoordinationOutcome outcome,
            String correlationId) {
        if (outcome.decision() != TrustedSearchCoordinationUseCase.Decision.FILL_AVAILABLE_SLOTS
                || run.mode() == com.cryptostrategy.platform.search.api.model.SearchRunMode.REPRODUCTION) {
            return;
        }
        int boundedWindow = Math.min(
                run.maxInFlight(),
                properties.concurrency().maxInFlightPerExperiment());
        allocations.fillAvailableSlots(new SearchCoordinationCommand(
                new com.cryptostrategy.platform.experiment.api.job.JobId(run.searchJobId().value()),
                new com.cryptostrategy.platform.experiment.api.ExperimentId(run.experimentId().value()),
                boundedWindow,
                properties.concurrency().maxInFlight(),
                1,
                correlationId));
    }
}

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

/** Adapter Worker mỏng; durable application port quyết định allocation và progress. */
public class SearchCoordinator {
    private final SearchCandidateAllocationUseCase allocations;
    private final WorkerProperties properties;
    private final SearchRunStore runs;
    private final TrustedSearchCoordinationUseCase trusted;
    private final SearchModuleFactory.Components search;

    public SearchCoordinator(
            SearchCandidateAllocationUseCase allocations,
            WorkerProperties properties) {
        this.allocations = Objects.requireNonNull(allocations, "allocations");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.runs = null;
        this.trusted = null;
        this.search = null;
    }

    public SearchCoordinator(
            SearchCandidateAllocationUseCase allocations,
            WorkerProperties properties,
            SearchRunStore runs,
            TrustedSearchCoordinationUseCase trusted,
            SearchModuleFactory.Components search) {
        this.allocations = Objects.requireNonNull(allocations, "allocations");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.runs = Objects.requireNonNull(runs, "runs");
        this.trusted = Objects.requireNonNull(trusted, "trusted");
        this.search = Objects.requireNonNull(search, "search");
    }

    public SearchCoordinationResult coordinate(SearchRequestPayload request, String correlationId) {
        Objects.requireNonNull(request, "request");
        if (runs != null) {
            SearchRun durable = runs.findBySearchJobId(request.searchJobId().value())
                    .orElseThrow(() -> new IllegalArgumentException("Search Run is inaccessible"));
            if (!durable.experimentRef().equals(request.experimentId().value())) {
                throw new IllegalArgumentException("Search Run is inaccessible");
            }
            search.requireGenerator(durable.generatorId(), durable.generatorVersion());
            if (durable.status().isTerminal()) {
                return new SearchCoordinationResult(
                        durable.searchRunId(), (int) durable.nextGenerationIndex(), 0, 0, 0,
                        durable.status());
            }
            var lifecycle = trusted.reconcileRun(new TrustedSearchCoordinationUseCase.ReconciliationTrigger(
                    new com.cryptostrategy.platform.experiment.api.ExperimentId(durable.experimentRef()), Instant.now(), correlationId));
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
        }
        int boundedWindow = Math.min(
                request.concurrencyHint(),
                properties.concurrency().maxInFlightPerExperiment());
        return allocations.fillAvailableSlots(new SearchCoordinationCommand(
                new com.cryptostrategy.platform.experiment.api.job.JobId(request.searchJobId().value()),
                new com.cryptostrategy.platform.experiment.api.ExperimentId(request.experimentId().value()),
                boundedWindow,
                request.topKTarget(),
                correlationId));
    }

    public TrustedSearchCoordinationUseCase.CoordinationOutcome complete(
            TrustedSearchCoordinationUseCase.CompletionTrigger trigger) {
        requireDurableRun(trigger.experimentId().value());
        return requireTrusted().reconcileCompletion(trigger);
    }

    public TrustedSearchCoordinationUseCase.CoordinationOutcome stop(
            String experimentId, Instant requestedAt, String correlationId) {
        requireDurableRun(experimentId);
        return requireTrusted().requestStop(
                new TrustedSearchCoordinationUseCase.StopTrigger(new com.cryptostrategy.platform.experiment.api.ExperimentId(experimentId), requestedAt, correlationId));
    }

    private SearchRun requireDurableRun(String experimentId) {
        if (runs == null) throw new IllegalStateException("Durable Search Run store is not configured");
        return runs.findByExperimentId(experimentId)
                .orElseThrow(() -> new IllegalArgumentException("Search Run is inaccessible"));
    }

    private TrustedSearchCoordinationUseCase requireTrusted() {
        if (trusted == null) throw new IllegalStateException("Trusted Search coordination is not configured");
        return trusted;
    }
}

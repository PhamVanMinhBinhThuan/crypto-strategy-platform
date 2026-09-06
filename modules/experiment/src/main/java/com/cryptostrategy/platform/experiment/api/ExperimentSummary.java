package com.cryptostrategy.platform.experiment.api;

import java.time.Instant;
import java.util.Objects;

/** Lightweight owner-scoped projection used by the experiment history page. */
public record ExperimentSummary(
        ExperimentId experimentId,
        String name,
        ExperimentStatus status,
        String datasetProvider,
        String datasetPair,
        String datasetTimeframe,
        long candleCount,
        int totalCandidates,
        int succeededCandidates,
        int failedCandidates,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt) {
    public ExperimentSummary {
        Objects.requireNonNull(experimentId, "experimentId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(datasetProvider, "datasetProvider");
        Objects.requireNonNull(datasetPair, "datasetPair");
        Objects.requireNonNull(datasetTimeframe, "datasetTimeframe");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public int processedCandidates() {
        return succeededCandidates + failedCandidates;
    }
}

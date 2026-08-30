package com.cryptostrategy.platform.experiment.api.provenance;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;

import java.time.Instant;
import java.util.Objects;

public record DatasetProvenanceSnapshot(
        DatasetVersionId datasetVersionId,
        String version,
        String checksum,
        String provider,
        String tradingPair,
        String timeframe,
        String normalizationVersion,
        Instant rangeStart,
        Instant rangeEnd,
        long candleCount
) {
    public DatasetProvenanceSnapshot {
        Objects.requireNonNull(datasetVersionId, "datasetVersionId cannot be null");
        Objects.requireNonNull(version, "version contract ID cannot be null");
        Objects.requireNonNull(checksum, "checksum cannot be null");
        Objects.requireNonNull(provider, "provider cannot be null");
        Objects.requireNonNull(tradingPair, "tradingPair cannot be null");
        Objects.requireNonNull(timeframe, "timeframe cannot be null");
        Objects.requireNonNull(normalizationVersion, "normalizationVersion cannot be null");
        Objects.requireNonNull(rangeStart, "rangeStart cannot be null");
        Objects.requireNonNull(rangeEnd, "rangeEnd cannot be null");
        if (rangeEnd.isBefore(rangeStart)) {
            throw new IllegalArgumentException("rangeEnd cannot be before rangeStart");
        }
        if (candleCount < 0) {
            throw new IllegalArgumentException("candleCount cannot be negative");
        }
    }
}

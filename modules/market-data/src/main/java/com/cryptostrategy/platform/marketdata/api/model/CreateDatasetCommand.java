package com.cryptostrategy.platform.marketdata.api.model;

public record CreateDatasetCommand(HistoricalCandleQuery query, String normalizationVersion, String version,
        java.util.UUID ownerUserId) {
    public CreateDatasetCommand { if (query == null || normalizationVersion == null || normalizationVersion.isBlank() || version == null || version.isBlank()) throw new IllegalArgumentException("Invalid Dataset command"); }
    public CreateDatasetCommand(HistoricalCandleQuery query, String normalizationVersion, String version) {
        this(query, normalizationVersion, version, null);
    }
}

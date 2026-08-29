package com.cryptostrategy.platform.marketdata.api.model;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import java.time.Instant;
import java.util.Objects;

public record DatasetSnapshot(DatasetVersionId datasetVersionId, String version, MarketProvider provider,
                              TradingPair tradingPair, Timeframe timeframe, String normalizationVersion,
                              Instant rangeStart, Instant rangeEnd, int candleCount, String checksum, Instant createdAt) {
    public DatasetSnapshot {
        Objects.requireNonNull(datasetVersionId); requireText(version, "version"); Objects.requireNonNull(provider);
        Objects.requireNonNull(tradingPair); Objects.requireNonNull(timeframe); requireText(normalizationVersion, "normalizationVersion");
        Objects.requireNonNull(rangeStart); Objects.requireNonNull(rangeEnd); Objects.requireNonNull(createdAt);
        if (!rangeStart.isBefore(rangeEnd) || candleCount < 1 || !checksum.matches("^sha256:[0-9a-f]{64}$")) throw new IllegalArgumentException("Invalid Dataset snapshot");
    }
    private static void requireText(String value, String name) { if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required"); }
}

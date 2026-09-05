package com.cryptostrategy.platform.api.market;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.Instant;
import java.util.List;

public final class MarketDtos {
    private MarketDtos() {}

    public record CandleResponse(
            String pair,
            String timeframe,
            Instant openTime,
            Instant closeTime,
            String open,
            String high,
            String low,
            String close,
            String volume,
            boolean closed) {
        static CandleResponse from(Candle candle) {
            return new CandleResponse(
                    candle.key().tradingPair().canonicalSymbol(),
                    candle.key().timeframe().code(),
                    candle.key().openTime(),
                    candle.closeTime(),
                    candle.open().toPlainString(),
                    candle.high().toPlainString(),
                    candle.low().toPlainString(),
                    candle.close().toPlainString(),
                    candle.volume().toPlainString(),
                    candle.closed());
        }
    }

    public record CandlePage(
            List<CandleResponse> items, String nextCursor, boolean hasMore) {
        public CandlePage {
            items = List.copyOf(items);
        }
    }

    public record CreateDatasetRequest(
            String pair, String timeframe, Instant startTime, Instant endTime) {}

    public record DatasetResponse(
            @JsonSerialize(using = ToStringSerializer.class) DatasetVersionId datasetId,
            String version,
            String provider,
            String pair,
            String timeframe,
            String normalizationVersion,
            Instant startTime,
            Instant endTime,
            int membershipCount,
            String checksum,
            String status,
            Instant createdAt) {
        static DatasetResponse from(DatasetSnapshot snapshot) {
            return new DatasetResponse(
                    snapshot.datasetVersionId(),
                    snapshot.version(),
                    snapshot.provider().value(),
                    snapshot.tradingPair().canonicalSymbol(),
                    snapshot.timeframe().code(),
                    snapshot.normalizationVersion(),
                    snapshot.rangeStart(),
                    snapshot.rangeEnd(),
                    snapshot.candleCount(),
                    snapshot.checksum(),
                    "READY",
                    snapshot.createdAt());
        }
    }

    public record DatasetListResponse(List<DatasetResponse> items) {
        public DatasetListResponse {
            items = List.copyOf(items);
        }
    }
}

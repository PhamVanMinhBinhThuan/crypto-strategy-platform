package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatasetProvenanceBindingTest {

    @Test
    @DisplayName("DatasetProvenanceSnapshot binds directly to canonical F-003 DatasetVersionId and candle-v1 checksum")
    void f003DatasetBinding() {
        DatasetVersionId datasetVersionId = new DatasetVersionId("01ARZ3NDEKTSV4RRFFQ69G5FAV");
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-02T00:00:00Z");

        DatasetProvenanceSnapshot snapshot = new DatasetProvenanceSnapshot(
                datasetVersionId,
                "candle-v1",
                "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "BINANCE",
                "BTC/USDT",
                "1m",
                "normalization-v1",
                start,
                end,
                1440
        );

        assertThat(snapshot.datasetVersionId()).isEqualTo(datasetVersionId);
        assertThat(snapshot.version()).isEqualTo("candle-v1");
        assertThat(snapshot.candleCount()).isEqualTo(1440);
        assertThat(snapshot.tradingPair()).isEqualTo("BTC/USDT");

        assertThatThrownBy(() -> new DatasetProvenanceSnapshot(
                datasetVersionId, "candle-v1", "sha256:123", "BINANCE", "BTC/USDT", "1m", "v1", end, start, 10
        )).isInstanceOf(IllegalArgumentException.class);
    }
}

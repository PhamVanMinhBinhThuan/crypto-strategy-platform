package com.cryptostrategy.platform.marketdata.api.model;

import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.PAIR;
import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.START;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Test;

class DatasetSnapshotTest {
    @Test
    void snapshotContainsMetadataOnlyAndUsesStringContractVersion() {
        DatasetSnapshot snapshot = new DatasetSnapshot(
                new DatasetVersionId("01ARZ3NDEKTSV4RRFFQ69G5FAY"),
                "candle-v1",
                MarketProvider.BINANCE,
                PAIR,
                Timeframe.ONE_MINUTE,
                "binance-v1",
                START,
                START.plusSeconds(60),
                1,
                "sha256:" + "a".repeat(64),
                Instant.parse("2026-01-02T00:00:00Z"));

        assertEquals(String.class, Arrays.stream(DatasetSnapshot.class.getRecordComponents())
                .filter(component -> component.getName().equals("version"))
                .findFirst()
                .map(RecordComponent::getType)
                .orElseThrow());
        assertFalse(Arrays.stream(DatasetSnapshot.class.getRecordComponents())
                .map(RecordComponent::getType)
                .anyMatch(Collection.class::isAssignableFrom));
        assertEquals("candle-v1", snapshot.version());
    }
}

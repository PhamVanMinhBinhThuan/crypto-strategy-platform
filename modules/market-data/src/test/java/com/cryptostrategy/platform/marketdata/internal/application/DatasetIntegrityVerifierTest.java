package com.cryptostrategy.platform.marketdata.internal.application;

import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.domain.api.market.CandleId;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.marketdata.api.model.*;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetCandleReader;
import com.cryptostrategy.platform.marketdata.internal.checksum.CandleV1Checksum;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatasetIntegrityVerifierTest {
    private static final DatasetVersionId DATASET_ID = new DatasetVersionId("01ARZ3NDEKTSV4RRFFQ69G5FAY");

    @Test void verifiesMetadataUsingSuccessiveBoundedPages() {
        List<PersistedCandle> candles = List.of(
                new PersistedCandle(new CandleId("01ARZ3NDEKTSV4RRFFQ69G5FA0"), candle(0, "1")),
                new PersistedCandle(new CandleId("01ARZ3NDEKTSV4RRFFQ69G5FA1"), candle(1, "2")));
        List<Integer> requested = new ArrayList<>();
        DatasetCandleReader reader = (id, from, size) -> {
            requested.add(size);
            var member = new DatasetMembership(id, from, candles.get(from));
            return new CandleBatch(id, from, List.of(member), from + 1, from + 1 < candles.size());
        };
        CandleV1Checksum checksum = new CandleV1Checksum();
        DatasetSnapshot snapshot = snapshot(checksum.calculate(candles.stream().map(PersistedCandle::candle).toList()));
        assertTrue(new DatasetIntegrityVerifier(reader, checksum).verify(snapshot).valid());
        assertEquals(List.of(2, 1), requested);
    }

    @Test void rejectsChecksumMismatchWithoutReturningCanonicalEvidence() {
        DatasetCandleReader reader = (id, from, size) -> new CandleBatch(id, from,
                List.of(new DatasetMembership(id, from,
                        new PersistedCandle(new CandleId("01ARZ3NDEKTSV4RRFFQ69G5FA0"), candle(from, Integer.toString(from + 1))))),
                from + 1, from == 0);
        assertFalse(new DatasetIntegrityVerifier(reader, new CandleV1Checksum())
                .verify(snapshot("sha256:" + "0".repeat(64))).valid());
    }

    private static DatasetSnapshot snapshot(String digest) {
        return new DatasetSnapshot(DATASET_ID, CandleV1Checksum.VERSION, MarketProvider.BINANCE, PAIR,
                Timeframe.ONE_MINUTE, "binance-v1", START, START.plusSeconds(120), 2, digest,
                Instant.parse("2026-01-02T00:00:00Z"));
    }
}

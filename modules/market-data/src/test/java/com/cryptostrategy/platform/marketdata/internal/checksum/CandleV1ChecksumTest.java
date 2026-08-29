package com.cryptostrategy.platform.marketdata.internal.checksum;

import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.candle;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CandleV1ChecksumTest {
    private final CandleV1Checksum checksum = new CandleV1Checksum();

    @Test void checksumIsOrderIndependentAndIncrementallyReproducible() {
        String expected = checksum.calculate(List.of(candle(0, "1.0"), candle(1, "2.00")));
        assertEquals(expected, checksum.calculate(List.of(candle(1, "2"), candle(0, "1.000"))));
        CandleV1Checksum.Accumulator accumulator = checksum.accumulator();
        accumulator.add(candle(0, "1"));
        accumulator.add(candle(1, "2"));
        assertEquals(expected, accumulator.finish());
        assertTrue(expected.matches("sha256:[0-9a-f]{64}"));
    }

    @Test void canonicalChangeChangesDigest() {
        assertNotEquals(checksum.calculate(List.of(candle(0, "1"))), checksum.calculate(List.of(candle(0, "2"))));
    }
}

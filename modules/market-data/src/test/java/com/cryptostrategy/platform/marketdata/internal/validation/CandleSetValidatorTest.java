package com.cryptostrategy.platform.marketdata.internal.validation;

import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandleSetValidatorTest {
    @Test void sortsAndCollapsesScaleEquivalentDuplicates() {
        assertEquals(List.of(candle(0, "1"), candle(1, "2")),
                CandleSetValidator.normalizeComplete(query(2), List.of(candle(1, "2.00"), candle(0, "1"), candle(0, "1.000"))));
    }
    @Test void rejectsGapAndConflictingDuplicate() {
        MarketDataException gap = assertThrows(MarketDataException.class,
                () -> CandleSetValidator.normalizeComplete(query(2), List.of(candle(0, "1"))));
        assertEquals(MarketDataErrorCode.MARKET_DATA_GAP, gap.code());
        MarketDataException conflict = assertThrows(MarketDataException.class,
                () -> CandleSetValidator.normalizeComplete(query(1), List.of(candle(0, "1"), candle(0, "2"))));
        assertEquals(MarketDataErrorCode.MARKET_DATA_INTEGRITY_CONFLICT, conflict.code());
    }
}

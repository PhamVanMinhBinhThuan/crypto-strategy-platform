package com.cryptostrategy.platform.marketdata.api.model;

import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.candle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cryptostrategy.platform.domain.api.market.CandleId;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandleBatchTest {
    private static final DatasetVersionId DATASET_ID =
            new DatasetVersionId("01ARZ3NDEKTSV4RRFFQ69G5FAY");

    @Test
    void enforcesContiguousZeroBasedContinuation() {
        DatasetMembership first = new DatasetMembership(
                DATASET_ID,
                5,
                new PersistedCandle(new CandleId("01ARZ3NDEKTSV4RRFFQ69G5FA0"), candle(0, "1")));
        CandleBatch batch = new CandleBatch(DATASET_ID, 5, List.of(first), 6, true);

        assertEquals(6, batch.nextSequence());
        assertThrows(IllegalArgumentException.class,
                () -> new CandleBatch(DATASET_ID, 5, List.of(first), 7, true));
        assertThrows(IllegalArgumentException.class,
                () -> new CandleBatch(DATASET_ID, 4, List.of(first), 5, true));
    }
}

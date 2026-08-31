package com.cryptostrategy.platform.domain.api.market;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class MarketUlidTest {
    private static final String ID="01ARZ3NDEKTSV4RRFFQ69G5FAV";
    @Test void roundTripsCanonicalTypedIds(){assertEquals(ID,new AssetId(ID).value());assertEquals(ID,new TradingPairId(ID).value());assertEquals(ID,new CandleId(ID).value());assertEquals(ID,new DatasetVersionId(ID).value());}
    @Test void rejectsLowercaseUuidAndForbiddenCharacters(){assertThrows(IllegalArgumentException.class,()->new AssetId(ID.toLowerCase()));assertThrows(IllegalArgumentException.class,()->new DatasetVersionId("550e8400-e29b-41d4-a716-446655440000"));assertThrows(IllegalArgumentException.class,()->new CandleId("01ARZ3NDEKTSV4RRFFQ69G5FAI"));}
}

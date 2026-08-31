package com.cryptostrategy.platform.domain.api.market;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.Test;
class TimeframeTest {
    @Test void exposesEightCanonicalUtcIntervals(){assertArrayEquals(new String[]{"1m","5m","15m","30m","1h","2h","4h","1d"},java.util.Arrays.stream(Timeframe.values()).map(Timeframe::code).toArray(String[]::new));Instant midnight=Instant.parse("2026-01-01T00:00:00Z");for(Timeframe timeframe:Timeframe.values()){assertTrue(timeframe.isAligned(midnight));assertEquals(midnight.plus(timeframe.duration()),timeframe.next(midnight));}assertFalse(Timeframe.ONE_MINUTE.isAligned(midnight.plusMillis(1)));}
}

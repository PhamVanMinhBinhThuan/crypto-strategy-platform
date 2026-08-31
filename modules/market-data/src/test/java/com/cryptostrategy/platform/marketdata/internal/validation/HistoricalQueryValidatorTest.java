package com.cryptostrategy.platform.marketdata.internal.validation;
import static com.cryptostrategy.platform.marketdata.support.MarketFixtures.*;import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.domain.api.market.*;import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;import java.time.Instant;import org.junit.jupiter.api.Test;
class HistoricalQueryValidatorTest {
 @Test void acceptsExplicitAlignedHalfOpenScope(){HistoricalCandleQuery value=query(2);assertEquals(START,value.startTime());assertEquals(START.plusSeconds(120),value.endTime());assertEquals(MarketProvider.BINANCE,value.provider());}
 @Test void rejectsUnalignedEmptyAndInvalidBounds(){assertThrows(IllegalArgumentException.class,()->new HistoricalCandleQuery(MarketProvider.BINANCE,PAIR,Timeframe.ONE_MINUTE,START.plusMillis(1),START.plusSeconds(60),START.plusSeconds(60),100,1));assertThrows(IllegalArgumentException.class,()->new HistoricalCandleQuery(MarketProvider.BINANCE,PAIR,Timeframe.ONE_MINUTE,START,START,START,100,1));assertThrows(IllegalArgumentException.class,()->new HistoricalCandleQuery(MarketProvider.BINANCE,PAIR,Timeframe.ONE_MINUTE,START,START.plusSeconds(60),START.plusSeconds(60),0,1));}
}

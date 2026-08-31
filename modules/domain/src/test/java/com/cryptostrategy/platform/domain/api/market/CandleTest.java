package com.cryptostrategy.platform.domain.api.market;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;import java.time.Instant;import java.util.Optional;
import org.junit.jupiter.api.Test;
class CandleTest {
    @Test void enforcesIdentityAlignmentExactOhlcvAndBoundaries(){Instant open=Instant.parse("2026-01-01T00:00:00Z");TradingPair pair=pair();CandleKey key=new CandleKey(MarketProvider.BINANCE,pair,Timeframe.ONE_MINUTE,open);Candle candle=new Candle(key,open.plusSeconds(60),d("1.0"),d("2"),d("0.5"),d("1.5"),d("10.00"),true);assertEquals(BigDecimal.ONE,candle.open());assertEquals(key,candle.key());assertThrows(IllegalArgumentException.class,()->new Candle(key,open.plusSeconds(59),d("1"),d("2"),d("0.5"),d("1.5"),d("10"),true));assertThrows(IllegalArgumentException.class,()->new Candle(key,open.plusSeconds(60),d("3"),d("2"),d("0.5"),d("1.5"),d("10"),true));assertFalse(new Candle(key,open.plusSeconds(60),d("1"),d("2"),d("0.5"),d("1.5"),d("10"),false).closed());}
    private static BigDecimal d(String value){return new BigDecimal(value);}private static TradingPair pair(){return new TradingPair(new TradingPairId("01ARZ3NDEKTSV4RRFFQ69G5FAX"),new Asset(new AssetId("01ARZ3NDEKTSV4RRFFQ69G5FAV"),new AssetSymbol("BTC"),Optional.empty(),true),new Asset(new AssetId("01ARZ3NDEKTSV4RRFFQ69G5FAW"),new AssetSymbol("USDT"),Optional.empty(),true),true);}
}

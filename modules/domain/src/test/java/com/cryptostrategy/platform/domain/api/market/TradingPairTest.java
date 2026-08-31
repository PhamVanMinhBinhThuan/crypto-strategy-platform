package com.cryptostrategy.platform.domain.api.market;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
class TradingPairTest {
    @Test void retainsDistinctBaseAndQuoteAndDerivesCanonicalSymbol(){Asset base=asset("01ARZ3NDEKTSV4RRFFQ69G5FAV","BTC");Asset quote=asset("01ARZ3NDEKTSV4RRFFQ69G5FAW","USDT");TradingPair pair=new TradingPair(new TradingPairId("01ARZ3NDEKTSV4RRFFQ69G5FAX"),base,quote,true);assertSame(base,pair.baseAsset());assertSame(quote,pair.quoteAsset());assertEquals("BTC/USDT",pair.canonicalSymbol());assertThrows(IllegalArgumentException.class,()->new TradingPair(pair.tradingPairId(),base,base,true));}
    private static Asset asset(String id,String symbol){return new Asset(new AssetId(id),new AssetSymbol(symbol),Optional.empty(),true);}
}

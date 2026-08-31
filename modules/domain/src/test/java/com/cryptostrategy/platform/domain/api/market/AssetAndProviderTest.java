package com.cryptostrategy.platform.domain.api.market;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class AssetAndProviderTest {
    @Test void canonicalNamesAreUppercase(){assertEquals("BTC",new AssetSymbol("BTC").value());assertEquals("BINANCE",new MarketProvider("BINANCE").value());assertThrows(IllegalArgumentException.class,()->new AssetSymbol("btc"));assertThrows(IllegalArgumentException.class,()->new MarketProvider("binance"));}
}

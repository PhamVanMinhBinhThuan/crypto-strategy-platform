package com.cryptostrategy.platform.marketdata.internal.provider.binance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BinanceErrorTranslatorTest {
    private final BinanceErrorTranslator translator = new BinanceErrorTranslator();

    @ParameterizedTest
    @CsvSource({
            "400,INVALID_MARKET_QUERY",
            "418,MARKET_PROVIDER_RATE_LIMITED",
            "429,MARKET_PROVIDER_RATE_LIMITED",
            "500,MARKET_PROVIDER_UNAVAILABLE",
            "503,MARKET_PROVIDER_UNAVAILABLE"
    })
    void translatesProviderStatusWithoutLeakingPayload(int status, MarketDataErrorCode expected) {
        var error = translator.translate(status);
        assertEquals(expected, error.code());
        assertEquals(Integer.toString(status), error.context().get("status"));
    }
}

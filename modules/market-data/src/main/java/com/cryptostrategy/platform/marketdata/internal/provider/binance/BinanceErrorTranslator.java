package com.cryptostrategy.platform.marketdata.internal.provider.binance;

import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import java.util.Map;

public final class BinanceErrorTranslator {
    public MarketDataException translate(int status) {
        if (status == 418 || status == 429) return new MarketDataException(MarketDataErrorCode.MARKET_PROVIDER_RATE_LIMITED, "Provider rate limited", Map.of("status", Integer.toString(status)));
        if (status >= 500) return new MarketDataException(MarketDataErrorCode.MARKET_PROVIDER_UNAVAILABLE, "Provider unavailable", Map.of("status", Integer.toString(status)));
        return new MarketDataException(MarketDataErrorCode.INVALID_MARKET_QUERY, "Provider rejected market query", Map.of("status", Integer.toString(status)));
    }
}

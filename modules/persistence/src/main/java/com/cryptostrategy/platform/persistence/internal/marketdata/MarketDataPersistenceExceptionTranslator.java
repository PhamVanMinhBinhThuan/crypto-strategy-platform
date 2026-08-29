package com.cryptostrategy.platform.persistence.internal.marketdata;

import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import java.util.Map;

public final class MarketDataPersistenceExceptionTranslator {
    public MarketDataException translate(RuntimeException exception) {
        if (exception instanceof MarketDataException marketDataException) return marketDataException;
        return new MarketDataException(MarketDataErrorCode.MARKET_DATA_INTEGRITY_CONFLICT, "Market persistence operation failed", Map.of(), exception);
    }
}

package com.cryptostrategy.platform.marketdata.api.port.out;

import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.marketdata.api.model.PersistedCandle;
import java.time.Instant;
import java.util.List;

public interface ClosedCandleStore {
    PersistedCandle saveClosed(Candle candle);

    List<PersistedCandle> saveClosedBatch(List<Candle> candles);

    List<PersistedCandle> findRange(
            MarketProvider provider,
            TradingPairId tradingPairId,
            Timeframe timeframe,
            Instant startInclusive,
            Instant endExclusive);
}

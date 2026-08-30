package com.cryptostrategy.platform.marketdata.api.model;

import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import java.util.Objects;

public record RealtimeCandleQuery(MarketProvider provider, TradingPair tradingPair, Timeframe timeframe) {
    public RealtimeCandleQuery { Objects.requireNonNull(provider); Objects.requireNonNull(tradingPair); Objects.requireNonNull(timeframe); }
}

package com.cryptostrategy.platform.marketdata.api.port.in;

import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import java.util.Optional;

/** Reads one configured canonical pair by its opaque public identity. */
@FunctionalInterface
public interface GetTradingPairUseCase {
    Optional<TradingPair> getTradingPair(TradingPairId tradingPairId);
}

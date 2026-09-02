package com.cryptostrategy.platform.marketdata.api.port.in;

import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.TradingPair;

/** Resolves one configured canonical pair without exposing reference-data persistence. */
@FunctionalInterface
public interface ResolveTradingPairUseCase {
    TradingPair resolveTradingPair(AssetSymbol baseAsset, AssetSymbol quoteAsset);
}

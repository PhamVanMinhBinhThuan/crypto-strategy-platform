package com.cryptostrategy.platform.marketdata.api.port.out;

import com.cryptostrategy.platform.domain.api.market.Asset;
import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import java.util.Optional;

public interface MarketReferenceDataStore {
    Asset resolveAsset(Asset asset);

    TradingPair resolveTradingPair(TradingPair tradingPair);

    Optional<TradingPair> findTradingPair(TradingPairId tradingPairId);

    Optional<Asset> findAsset(AssetSymbol symbol);
}

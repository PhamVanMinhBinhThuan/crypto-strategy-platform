package com.cryptostrategy.platform.domain.api.market;

import java.util.Objects;

public record TradingPair(TradingPairId tradingPairId, Asset baseAsset, Asset quoteAsset, boolean active) {
    public TradingPair {
        Objects.requireNonNull(tradingPairId, "tradingPairId");
        Objects.requireNonNull(baseAsset, "baseAsset");
        Objects.requireNonNull(quoteAsset, "quoteAsset");
        if (baseAsset.assetId().equals(quoteAsset.assetId())) throw new IllegalArgumentException("Base and quote assets must differ");
    }
    public String canonicalSymbol() { return baseAsset.symbol() + "/" + quoteAsset.symbol(); }
}

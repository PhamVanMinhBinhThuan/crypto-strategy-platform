package com.cryptostrategy.platform.marketdata.internal.application;

import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.port.in.ResolveTradingPairUseCase;
import com.cryptostrategy.platform.marketdata.api.port.in.GetTradingPairUseCase;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketReferenceDataStore;
import java.util.Objects;

public final class MarketReferenceDataQueryService implements ResolveTradingPairUseCase, GetTradingPairUseCase {
    private final MarketReferenceDataStore references;

    public MarketReferenceDataQueryService(MarketReferenceDataStore references) {
        this.references = Objects.requireNonNull(references, "references");
    }

    @Override
    public TradingPair resolveTradingPair(AssetSymbol baseAsset, AssetSymbol quoteAsset) {
        Objects.requireNonNull(baseAsset, "baseAsset");
        Objects.requireNonNull(quoteAsset, "quoteAsset");
        if (baseAsset.equals(quoteAsset)) {
            throw invalidPair();
        }
        return references.findTradingPair(baseAsset, quoteAsset)
                .filter(TradingPair::active)
                .filter(pair -> pair.baseAsset().active() && pair.quoteAsset().active())
                .orElseThrow(MarketReferenceDataQueryService::invalidPair);
    }

    @Override
    public java.util.Optional<TradingPair> getTradingPair(
            com.cryptostrategy.platform.domain.api.market.TradingPairId tradingPairId) {
        Objects.requireNonNull(tradingPairId, "tradingPairId");
        return references.findTradingPair(tradingPairId)
                .filter(TradingPair::active)
                .filter(pair -> pair.baseAsset().active() && pair.quoteAsset().active());
    }

    private static MarketDataException invalidPair() {
        return new MarketDataException(
                MarketDataErrorCode.INVALID_MARKET_QUERY,
                "The requested trading pair is not configured");
    }
}

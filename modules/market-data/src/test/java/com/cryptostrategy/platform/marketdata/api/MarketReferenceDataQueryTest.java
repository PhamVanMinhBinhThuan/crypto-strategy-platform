package com.cryptostrategy.platform.marketdata.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cryptostrategy.platform.domain.api.market.Asset;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketReferenceDataStore;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MarketReferenceDataQueryTest {
    private static final AssetSymbol BTC = new AssetSymbol("BTC");
    private static final AssetSymbol USDT = new AssetSymbol("USDT");
    private static final TradingPair PAIR = pair(true);

    @Test
    void resolvesOnlyConfiguredActiveCanonicalPair() {
        var query = MarketDataModuleFactory.referenceData(references(Optional.of(PAIR)));

        assertEquals(PAIR, query.resolveTradingPair(BTC, USDT));
    }

    @Test
    void missingInactiveAndSelfPairUseStableMarketValidationError() {
        for (Optional<TradingPair> result :
                java.util.List.of(Optional.<TradingPair>empty(), Optional.of(pair(false)))) {
            var query = MarketDataModuleFactory.referenceData(references(result));

            MarketDataException failure = assertThrows(
                    MarketDataException.class,
                    () -> query.resolveTradingPair(BTC, USDT));
            assertEquals(MarketDataErrorCode.INVALID_MARKET_QUERY, failure.code());
        }

        var query = MarketDataModuleFactory.referenceData(references(Optional.of(PAIR)));
        MarketDataException selfPair = assertThrows(
                MarketDataException.class,
                () -> query.resolveTradingPair(BTC, BTC));
        assertEquals(MarketDataErrorCode.INVALID_MARKET_QUERY, selfPair.code());
    }

    private static MarketReferenceDataStore references(Optional<TradingPair> result) {
        return new MarketReferenceDataStore() {
            @Override
            public Asset resolveAsset(Asset asset) {
                throw new UnsupportedOperationException();
            }

            @Override
            public TradingPair resolveTradingPair(TradingPair tradingPair) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<TradingPair> findTradingPair(TradingPairId tradingPairId) {
                return Optional.empty();
            }

            @Override
            public Optional<TradingPair> findTradingPair(
                    AssetSymbol baseAsset, AssetSymbol quoteAsset) {
                return baseAsset.equals(BTC) && quoteAsset.equals(USDT)
                        ? result
                        : Optional.empty();
            }

            @Override
            public Optional<Asset> findAsset(AssetSymbol symbol) {
                return Optional.empty();
            }
        };
    }

    private static TradingPair pair(boolean active) {
        Asset base = new Asset(
                new AssetId("01J00000000000000000000101"), BTC, Optional.empty(), active);
        Asset quote = new Asset(
                new AssetId("01J00000000000000000000102"), USDT, Optional.empty(), active);
        return new TradingPair(
                new TradingPairId("01J00000000000000000000103"),
                base,
                quote,
                active);
    }
}

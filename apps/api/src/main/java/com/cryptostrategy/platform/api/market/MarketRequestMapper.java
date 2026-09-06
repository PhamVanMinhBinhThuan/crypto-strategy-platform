package com.cryptostrategy.platform.api.market;

import com.cryptostrategy.platform.api.config.MarketDataProperties;
import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.marketdata.api.model.HistoricalCandleQuery;
import com.cryptostrategy.platform.marketdata.api.port.in.ResolveTradingPairUseCase;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
final class MarketRequestMapper {
    private final ResolveTradingPairUseCase pairs;
    private final MarketDataProperties properties;

    MarketRequestMapper(
            ResolveTradingPairUseCase pairs, MarketDataProperties properties) {
        this.pairs = pairs;
        this.properties = properties;
    }

    MarketRange range(
            String pair, String timeframe, Instant startTime, Instant endTime) {
        try {
            String[] assets = pair == null ? new String[0] : pair.split("/", -1);
            if (assets.length != 2) {
                throw new IllegalArgumentException("Pair must contain base and quote assets");
            }
            AssetSymbol base = new AssetSymbol(assets[0]);
            AssetSymbol quote = new AssetSymbol(assets[1]);
            Timeframe canonicalTimeframe = Timeframe.fromCode(timeframe);
            if (startTime == null
                    || endTime == null
                    || !startTime.isBefore(endTime)
                    || endTime.isAfter(Instant.now())
                    || !canonicalTimeframe.isAligned(startTime)
                    || !canonicalTimeframe.isAligned(endTime)) {
                throw new IllegalArgumentException("Market range is invalid");
            }
            TradingPair tradingPair = pairs.resolveTradingPair(base, quote);
            if (tradingPair == null) {
                throw new IllegalStateException("Trading pair resolver returned no result");
            }
            return new MarketRange(
                    new MarketProvider(properties.provider().toUpperCase(Locale.ROOT)),
                    tradingPair,
                    canonicalTimeframe,
                    startTime,
                    endTime);
        } catch (MarketDataException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new MarketDataException(
                    MarketDataErrorCode.INVALID_MARKET_QUERY,
                    "The market data query is invalid");
        }
    }

    HistoricalCandleQuery query(MarketRange range, Instant pageStart, Instant pageEnd) {
        MarketDataProperties.Binance pagination = properties.binance();
        int pageSize = pagination == null ? 1000 : pagination.pageSize();
        int maxPages = pagination == null ? 1000 : pagination.maxPages();
        return new HistoricalCandleQuery(
                range.provider(),
                range.tradingPair(),
                range.timeframe(),
                pageStart,
                pageEnd,
                pageEnd,
                pageSize,
                maxPages);
    }

    record MarketRange(
            MarketProvider provider,
            TradingPair tradingPair,
            Timeframe timeframe,
            Instant startTime,
            Instant endTime) {}
}

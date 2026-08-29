package com.cryptostrategy.platform.marketdata.internal.observability;

import com.cryptostrategy.platform.domain.api.market.CandleKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MarketDataEventLogger {
    private static final Logger LOG = LoggerFactory.getLogger(MarketDataEventLogger.class);
    public void providerLifecycle(String event, CandleKey key, int attempt) {
        LOG.info("marketDataEvent={} provider={} pair={} timeframe={} attempt={}", event, key.provider().value(), key.tradingPair().canonicalSymbol(), key.timeframe().code(), attempt);
    }
    public void datasetLifecycle(String event, String datasetId, String checksum) {
        LOG.info("marketDataEvent={} datasetId={} checksum={}", event, datasetId, checksum);
    }
    public void persistenceConflict(CandleKey key) {
        LOG.warn("marketDataEvent=persistenceConflict provider={} pair={} timeframe={} openTime={}", key.provider().value(), key.tradingPair().canonicalSymbol(), key.timeframe().code(), key.openTime());
    }
}

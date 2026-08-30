package com.cryptostrategy.platform.api.config;

import com.cryptostrategy.platform.marketdata.api.MarketDataModuleFactory;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketDataProvider;
import com.cryptostrategy.platform.persistence.api.MarketDataPersistenceFactory;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MarketDataProperties.class)
public class MarketDataConfiguration {
    @Bean MarketDataProvider marketDataProvider(MarketDataProperties properties) {
        if ("fixture".equalsIgnoreCase(properties.provider())) return MarketDataModuleFactory.fixtureProvider(java.util.List.of());
        MarketDataProperties.Binance binance = properties.binance();
        MarketDataProperties.Retry retry = binance.retry();
        return MarketDataModuleFactory.binanceProvider(binance.restBaseUrl(), binance.websocketBaseUrl(),
                binance.connectTimeout(), binance.requestTimeout(), properties.normalizationVersion(),
                retry.maxAttempts(), retry.initialDelay(), retry.maxDelay());
    }
    @Bean MarketDataPersistenceFactory.Components marketDataPersistence(DataSource dataSource) { return MarketDataPersistenceFactory.create(dataSource); }
    @Bean MarketDataModuleFactory.Components marketDataModule(MarketDataProvider provider,
            MarketDataPersistenceFactory.Components persistence, MarketDataProperties properties) {
        MarketDataProperties.Binance binance = properties.binance();
        if (binance == null) {
            return MarketDataModuleFactory.create(provider, persistence.candles(), persistence.datasets(),
                    persistence.reader(), Clock.systemUTC());
        }
        MarketDataProperties.Reconnect reconnect = binance.reconnect();
        MarketDataModuleFactory.RecoverySettings recovery = new MarketDataModuleFactory.RecoverySettings(
                reconnect.maxAttempts(), reconnect.initialDelay(), reconnect.maxDelay(),
                binance.pageSize(), binance.maxPages());
        return MarketDataModuleFactory.create(provider, persistence.candles(), persistence.datasets(),
                persistence.reader(), Clock.systemUTC(), recovery);
    }
}

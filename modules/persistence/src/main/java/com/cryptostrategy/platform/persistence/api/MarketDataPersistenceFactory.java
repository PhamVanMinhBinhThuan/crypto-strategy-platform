package com.cryptostrategy.platform.persistence.api;

import com.cryptostrategy.platform.marketdata.api.port.out.ClosedCandleStore;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetCandleReader;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetStore;
import com.cryptostrategy.platform.marketdata.api.port.out.MarketReferenceDataStore;
import com.cryptostrategy.platform.persistence.internal.marketdata.JdbcCandleStoreAdapter;
import com.cryptostrategy.platform.persistence.internal.marketdata.JdbcDatasetCandleReader;
import com.cryptostrategy.platform.persistence.internal.marketdata.JdbcDatasetStoreAdapter;
import com.cryptostrategy.platform.persistence.internal.marketdata.JdbcMarketReferenceDataAdapter;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class MarketDataPersistenceFactory {
    private MarketDataPersistenceFactory() { }
    public static Components create(DataSource dataSource) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource); JdbcMarketReferenceDataAdapter references = new JdbcMarketReferenceDataAdapter(jdbc);
        JdbcCandleStoreAdapter candles = new JdbcCandleStoreAdapter(jdbc, references); JdbcDatasetCandleReader reader = new JdbcDatasetCandleReader(jdbc);
        JdbcDatasetStoreAdapter datasets = new JdbcDatasetStoreAdapter(jdbc, new TransactionTemplate(new DataSourceTransactionManager(dataSource)), references, candles, reader);
        return new Components(references, candles, datasets, reader);
    }
    public record Components(MarketReferenceDataStore references, ClosedCandleStore candles, DatasetStore datasets, DatasetCandleReader reader) { }
}

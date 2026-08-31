package com.cryptostrategy.platform.persistence.marketdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptostrategy.platform.domain.api.market.Asset;
import com.cryptostrategy.platform.domain.api.market.AssetId;
import com.cryptostrategy.platform.domain.api.market.AssetSymbol;
import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.domain.api.market.CandleKey;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.domain.api.market.MarketProvider;
import com.cryptostrategy.platform.domain.api.market.Timeframe;
import com.cryptostrategy.platform.domain.api.market.TradingPair;
import com.cryptostrategy.platform.domain.api.market.TradingPairId;
import com.cryptostrategy.platform.marketdata.api.model.DatasetFinalization;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.persistence.api.MarketDataPersistenceFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class MarketDataPersistenceIntegrationTest {
    private static DataSource dataSource;

    @BeforeAll
    static void connectToIsolatedTestDatabase() {
        String url = required("DATABASE_URL");
        String username = required("DATABASE_USERNAME");
        boolean local = url.contains("localhost") || url.contains("127.0.0.1");
        String testProjectRef = System.getenv("DATABASE_TEST_PROJECT_REF");
        boolean isolatedRemote = testProjectRef != null
                && !testProjectRef.isBlank()
                && username.endsWith("." + testProjectRef);
        if (!local && !isolatedRemote) {
            throw new IllegalStateException(
                    "Integration tests require local PostgreSQL or an explicitly identified Supabase test project");
        }
        dataSource = new DriverManagerDataSource(
                url,
                username,
                required("DATABASE_PASSWORD"));
    }

    @Test
    void finalizesAndReadsMembershipInBoundedPagesWithoutLeavingTestData() {
        MarketDataPersistenceFactory.Components components = MarketDataPersistenceFactory.create(dataSource);
        DatasetVersionId datasetId = DatasetVersionId.generate();
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        transaction.executeWithoutResult(status -> {
            DatasetFinalization finalization = finalization(datasetId, List.of(candle(0), candle(1)));
            DatasetSnapshot stored = components.datasets().finalizeAtomically(finalization);

            assertEquals(2, stored.candleCount());
            assertEquals(1, components.reader().readCandles(datasetId, 0, 1).members().size());
            assertTrue(components.reader().readCandles(datasetId, 0, 1).hasMore());
            assertFalse(components.reader().readCandles(datasetId, 1, 1).hasMore());
            assertTrue(components.reader().readCandles(datasetId, 2, 1).members().isEmpty());
            status.setRollbackOnly();
        });

        assertTrue(components.datasets().find(datasetId).isEmpty());
    }

    @Test
    void rollsBackCandleAndMetadataWhenMembershipFinalizationFails() {
        MarketDataPersistenceFactory.Components components = MarketDataPersistenceFactory.create(dataSource);
        DatasetVersionId datasetId = DatasetVersionId.generate();
        Candle duplicate = candle(0);

        assertThrows(RuntimeException.class,
                () -> components.datasets().finalizeAtomically(
                        finalization(datasetId, List.of(duplicate, duplicate))));

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertTrue(components.datasets().find(datasetId).isEmpty());
        assertEquals(0, jdbc.queryForObject(
                "select count(*) from market.asset where symbol in ('TSTBTC','TSTUSD')",
                Integer.class));
    }

    private static DatasetFinalization finalization(DatasetVersionId datasetId, List<Candle> candles) {
        TradingPair pair = pair();
        Instant start = candles.getFirst().key().openTime();
        DatasetSnapshot snapshot = new DatasetSnapshot(
                datasetId,
                "candle-v1",
                MarketProvider.BINANCE,
                pair,
                Timeframe.ONE_MINUTE,
                "integration-v1",
                start,
                start.plusSeconds(60L * candles.size()),
                candles.size(),
                "sha256:" + "b".repeat(64),
                Instant.parse("2026-08-30T00:00:00Z"));
        return new DatasetFinalization(snapshot, candles);
    }

    private static Candle candle(int minute) {
        TradingPair pair = pair();
        Instant openTime = Instant.parse("2026-08-01T00:00:00Z").plusSeconds(60L * minute);
        return new Candle(
                new CandleKey(MarketProvider.BINANCE, pair, Timeframe.ONE_MINUTE, openTime),
                openTime.plusSeconds(60),
                new BigDecimal("100"),
                new BigDecimal("110"),
                new BigDecimal("90"),
                new BigDecimal("105"),
                new BigDecimal("10"),
                true);
    }

    private static TradingPair pair() {
        return new TradingPair(
                new TradingPairId("01K3ZZZZZZZZZZZZZZZZZZZZZX"),
                new Asset(
                        new AssetId("01K3ZZZZZZZZZZZZZZZZZZZZZV"),
                        new AssetSymbol("TSTBTC"),
                        Optional.of("Integration BTC"),
                        true),
                new Asset(
                        new AssetId("01K3ZZZZZZZZZZZZZZZZZZZZZW"),
                        new AssetSymbol("TSTUSD"),
                        Optional.of("Integration USD"),
                        true),
                true);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing local database configuration: " + name);
        }
        return value;
    }
}

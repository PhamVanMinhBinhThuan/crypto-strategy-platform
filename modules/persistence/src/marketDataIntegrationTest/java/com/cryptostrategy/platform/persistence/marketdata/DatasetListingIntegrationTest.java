package com.cryptostrategy.platform.persistence.marketdata;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class DatasetListingIntegrationTest {
    private static final UUID OWNER_A = UUID.fromString("00000000-0000-4000-8000-000000000951");
    private static final UUID OWNER_B = UUID.fromString("00000000-0000-4000-8000-000000000952");
    private static final Instant CREATED = Instant.parse("2026-09-05T05:00:00Z");
    private static DataSource dataSource;

    @BeforeAll
    static void connectToIsolatedTestDatabase() {
        String url = required("DATABASE_URL");
        String username = required("DATABASE_USERNAME");
        boolean local = url.contains("localhost") || url.contains("127.0.0.1");
        String testProjectRef = System.getenv("DATABASE_TEST_PROJECT_REF");
        boolean isolatedRemote = testProjectRef != null && !testProjectRef.isBlank()
                && username.endsWith("." + testProjectRef);
        if (!local && !isolatedRemote) {
            throw new IllegalStateException(
                    "Integration tests require local PostgreSQL or an explicitly identified Supabase test project");
        }
        dataSource = new DriverManagerDataSource(url, username, required("DATABASE_PASSWORD"));
    }

    @Test
    void listsOnlyGrantedDatasetsInDeterministicRecentOrder() {
        var components = MarketDataPersistenceFactory.create(dataSource);
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transaction.executeWithoutResult(status -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.update("insert into auth.users(id) values (?),(?) on conflict do nothing", OWNER_A, OWNER_B);
            DatasetSnapshot first = components.datasets().finalizeAtomically(
                    finalization("01J00000000000000000000961", 'a', CREATED));
            DatasetSnapshot second = components.datasets().finalizeAtomically(
                    finalization("01J00000000000000000000962", 'b', CREATED.plusSeconds(1)));
            components.datasets().grantAccess(OWNER_A, first.datasetVersionId());
            components.datasets().grantAccess(OWNER_A, second.datasetVersionId());
            components.datasets().grantAccess(OWNER_B, first.datasetVersionId());

            assertThat(components.datasets().listRecent(OWNER_A, 10))
                    .extracting(item -> item.datasetVersionId().value())
                    .containsExactly(second.datasetVersionId().value(), first.datasetVersionId().value());
            assertThat(components.datasets().listRecent(OWNER_A, 1))
                    .extracting(item -> item.datasetVersionId().value())
                    .containsExactly(second.datasetVersionId().value());
            assertThat(components.datasets().listRecent(OWNER_B, 10))
                    .extracting(item -> item.datasetVersionId().value())
                    .containsExactly(first.datasetVersionId().value());
            status.setRollbackOnly();
        });
    }

    private static DatasetFinalization finalization(String id, char checksumToken, Instant createdAt) {
        TradingPair pair = pair();
        Instant start = Instant.parse("2026-09-01T00:00:00Z")
                .plusSeconds(checksumToken == 'a' ? 0 : 60);
        Candle candle = new Candle(
                new CandleKey(MarketProvider.BINANCE, pair, Timeframe.ONE_MINUTE, start),
                start.plusSeconds(60), new BigDecimal("100"), new BigDecimal("101"),
                new BigDecimal("99"), new BigDecimal("100"), BigDecimal.ONE, true);
        DatasetSnapshot snapshot = new DatasetSnapshot(new DatasetVersionId(id), "candle-v1",
                MarketProvider.BINANCE, pair, Timeframe.ONE_MINUTE, "f015-integration-v1",
                start, start.plusSeconds(60), 1,
                "sha256:" + String.valueOf(checksumToken).repeat(64), createdAt);
        return new DatasetFinalization(snapshot, List.of(candle));
    }

    private static TradingPair pair() {
        return new TradingPair(new TradingPairId("01J00000000000000000000963"),
                new Asset(new AssetId("01J00000000000000000000964"),
                        new AssetSymbol("F15BTC"), Optional.of("F-015 BTC"), true),
                new Asset(new AssetId("01J00000000000000000000965"),
                        new AssetSymbol("F15USD"), Optional.of("F-015 USD"), true), true);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing local database configuration: " + name);
        }
        return value;
    }
}

package com.cryptostrategy.platform.api.config;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cryptostrategy.platform.strategies.api.StrategyPlugins;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyCatalogSynchronization;
import com.cryptostrategy.platform.strategy.internal.registry.DefaultStrategyRegistry;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
class StrategyConfigurationTest {
    @Test void trustedPluginCompositionHasNoDeliveryEndpoint(){assertEquals(4,new DefaultStrategyRegistry(StrategyPlugins.trusted()).listAvailable().size());assertFalse(StrategyConfiguration.class.getName().toLowerCase().contains("controller"));}
    @Test void startupSynchronizesTheRuntimeCatalogOnPostgres() throws Exception {
        var synchronization=mock(StrategyCatalogSynchronization.class);
        var runner=new StrategyConfiguration().strategyCatalogStartup(synchronization,dataSource("PostgreSQL"));
        runner.run(null);
        verify(synchronization).synchronize();
    }
    @Test void startupLeavesNonPostgresTestDatasourceUntouched() throws Exception {
        var synchronization=mock(StrategyCatalogSynchronization.class);
        var runner=new StrategyConfiguration().strategyCatalogStartup(synchronization,dataSource("H2"));
        runner.run(null);
        verifyNoInteractions(synchronization);
    }
    private static DataSource dataSource(String productName) throws Exception {
        var metadata=mock(DatabaseMetaData.class);
        when(metadata.getDatabaseProductName()).thenReturn(productName);
        var connection=mock(Connection.class);
        when(connection.getMetaData()).thenReturn(metadata);
        var dataSource=mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        return dataSource;
    }
}

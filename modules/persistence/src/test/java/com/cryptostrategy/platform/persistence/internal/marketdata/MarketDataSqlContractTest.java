package com.cryptostrategy.platform.persistence.internal.marketdata;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MarketDataSqlContractTest {
    @Test void everyStatementIsSchemaQualifiedAndAppendOnly() throws Exception {
        for (var field : MarketDataSql.class.getFields()) {
            String sql = (String) field.get(null);
            assertTrue(sql.contains("market."), field.getName());
            assertFalse(sql.toLowerCase().startsWith("update "), field.getName());
            assertFalse(sql.toLowerCase().startsWith("delete "), field.getName());
        }
    }

    @Test void persistencePortsExposeNoMutationAfterFinalization() {
        assertFalse(Arrays.stream(com.cryptostrategy.platform.marketdata.api.port.out.DatasetStore.class.getMethods())
                .map(Method::getName).anyMatch(name -> name.startsWith("update") || name.startsWith("delete") || name.contains("Member")));
    }
}

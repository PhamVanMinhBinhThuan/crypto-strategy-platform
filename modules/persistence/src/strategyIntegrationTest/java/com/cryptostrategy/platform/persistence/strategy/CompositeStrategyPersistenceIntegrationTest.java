package com.cryptostrategy.platform.persistence.strategy;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
class CompositeStrategyPersistenceIntegrationTest {
    @Test void localSchemaHasCompositeIntegrityGuards() throws Exception {try(var connection=DriverManager.getConnection(System.getenv("DATABASE_URL"),System.getenv("DATABASE_USERNAME"),System.getenv("DATABASE_PASSWORD"));var statement=connection.prepareStatement("select count(*) from information_schema.table_constraints where constraint_schema='strategy' and constraint_name in ('user_strategy_version_source_valid','user_strategy_component_plugin_unique')")){try(var result=statement.executeQuery()){assertTrue(result.next());assertEquals(2,result.getInt(1));}}}
}

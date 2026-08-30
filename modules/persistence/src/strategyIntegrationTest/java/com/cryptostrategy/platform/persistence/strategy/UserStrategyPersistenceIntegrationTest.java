package com.cryptostrategy.platform.persistence.strategy;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
class UserStrategyPersistenceIntegrationTest {
    @Test void localSchemaContainsOwnerScopedImmutableStrategyTables() throws Exception {try(var connection=DriverManager.getConnection(System.getenv("DATABASE_URL"),System.getenv("DATABASE_USERNAME"),System.getenv("DATABASE_PASSWORD"));var statement=connection.prepareStatement("select count(*) from information_schema.tables where table_schema='strategy' and table_name in ('user_strategy','user_strategy_version','user_strategy_component')")){try(var result=statement.executeQuery()){assertTrue(result.next());assertEquals(3,result.getInt(1));}}}
}

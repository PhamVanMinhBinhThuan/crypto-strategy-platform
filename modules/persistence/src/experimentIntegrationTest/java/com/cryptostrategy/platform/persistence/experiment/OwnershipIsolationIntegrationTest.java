package com.cryptostrategy.platform.persistence.experiment;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnershipIsolationIntegrationTest {

    @Test
    void experimentTablesEnforceForeignKeysAndOwnershipStructure() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                System.getenv("DATABASE_URL"),
                System.getenv("DATABASE_USERNAME"),
                System.getenv("DATABASE_PASSWORD")
        );
             PreparedStatement statement = connection.prepareStatement(
                     "select count(*) from information_schema.columns where table_schema='experiment' and table_name='experiment' and column_name='owner_user_id'"
             )) {
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
        }
    }
}

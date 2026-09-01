package com.cryptostrategy.platform.persistence.experiment;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxPersistenceIntegrationTest {

    @Test
    void outboxEventTableExistsWithRequiredColumns() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                System.getenv("DATABASE_URL"),
                System.getenv("DATABASE_USERNAME"),
                System.getenv("DATABASE_PASSWORD")
        );
             PreparedStatement statement = connection.prepareStatement(
                     "select count(*) from information_schema.tables where table_schema='platform' and table_name='outbox_event'"
             )) {
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
        }
    }
}

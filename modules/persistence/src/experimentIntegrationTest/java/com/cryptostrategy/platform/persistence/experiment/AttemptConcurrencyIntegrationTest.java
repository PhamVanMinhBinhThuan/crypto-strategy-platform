package com.cryptostrategy.platform.persistence.experiment;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttemptConcurrencyIntegrationTest {

    @Test
    void executionAttemptTableHasUniqueJobAttemptConstraint() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                System.getenv("DATABASE_URL"),
                System.getenv("DATABASE_USERNAME"),
                System.getenv("DATABASE_PASSWORD")
        );
             PreparedStatement statement = connection.prepareStatement(
                     "select count(*) from information_schema.table_constraints where table_schema='experiment' and table_name='execution_attempt' and constraint_name='execution_job_attempt_unique'"
             )) {
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
        }
    }
}

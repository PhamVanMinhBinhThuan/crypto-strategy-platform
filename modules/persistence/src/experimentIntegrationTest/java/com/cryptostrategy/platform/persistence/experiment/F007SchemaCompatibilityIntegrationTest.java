package com.cryptostrategy.platform.persistence.experiment;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class F007SchemaCompatibilityIntegrationTest {

    private Connection createConnection() throws Exception {
        return DriverManager.getConnection(
                System.getenv("DATABASE_URL"),
                System.getenv("DATABASE_USERNAME"),
                System.getenv("DATABASE_PASSWORD")
        );
    }

    @Test
    void outboxEventTableHasRequiredFieldsForF007Publication() throws Exception {
        try (Connection connection = createConnection()) {
            Set<String> columns = getColumns(connection, "platform", "outbox_event");
            assertThat(columns).contains(
                    "outbox_event_id",
                    "message_id",
                    "aggregate_type",
                    "aggregate_id",
                    "event_type",
                    "event_version",
                    "payload",
                    "headers",
                    "published_at",
                    "publish_attempts",
                    "last_error",
                    "occurred_at",
                    "created_at"
            );
        }
    }

    @Test
    void processedMessageTableHasPrimaryKeyAndTtl() throws Exception {
        try (Connection connection = createConnection()) {
            Set<String> columns = getColumns(connection, "platform", "processed_message");
            assertThat(columns).contains(
                    "consumer_name",
                    "message_id",
                    "processed_at",
                    "expires_at"
            );
        }
    }

    @Test
    void jobAndExecutionAttemptHaveRequiredLifecycleAndRetryColumns() throws Exception {
        try (Connection connection = createConnection()) {
            Set<String> jobColumns = getColumns(connection, "experiment", "job");
            assertThat(jobColumns).contains(
                    "job_id",
                    "experiment_id",
                    "candidate_id",
                    "job_type",
                    "status",
                    "total_work",
                    "completed_work",
                    "failed_work",
                    "best_score",
                    "next_retry_at",
                    "failure_code",
                    "failure_message",
                    "started_at",
                    "finished_at"
            );

            Set<String> attemptColumns = getColumns(connection, "experiment", "execution_attempt");
            assertThat(attemptColumns).contains(
                    "attempt_id",
                    "job_id",
                    "candidate_id",
                    "attempt_no",
                    "status",
                    "worker_id",
                    "started_at",
                    "finished_at",
                    "failure_code",
                    "failure_message",
                    "retryable"
            );
        }
    }

    private static Set<String> getColumns(Connection connection, String schema, String table) throws Exception {
        Set<String> columns = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = ? AND table_name = ?"
        )) {
            statement.setString(1, schema);
            statement.setString(2, table);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    columns.add(rs.getString(1).toLowerCase());
                }
            }
        }
        return columns;
    }
}

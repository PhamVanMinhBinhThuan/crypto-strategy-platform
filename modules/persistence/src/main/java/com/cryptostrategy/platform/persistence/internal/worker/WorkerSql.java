package com.cryptostrategy.platform.persistence.internal.worker;

public final class WorkerSql {
    private WorkerSql() {}

    public static final String SELECT_UNPUBLISHED_OUTBOX =
            "SELECT outbox_event_id, message_id, aggregate_type, aggregate_id, event_type, " +
            "       event_version, payload, headers, published_at, publish_attempts, last_error, " +
            "       occurred_at, created_at " +
            "FROM platform.outbox_event " +
            "WHERE published_at IS NULL " +
            "ORDER BY occurred_at ASC, outbox_event_id ASC " +
            "LIMIT ?";

    public static final String UPDATE_OUTBOX_SUCCESS =
            "UPDATE platform.outbox_event " +
            "SET published_at = ?, last_error = NULL " +
            "WHERE outbox_event_id = ?";

    public static final String UPDATE_OUTBOX_FAILURE =
            "UPDATE platform.outbox_event " +
            "SET publish_attempts = publish_attempts + 1, last_error = ? " +
            "WHERE outbox_event_id = ?";

    public static final String SELECT_IS_MESSAGE_PROCESSED =
            "SELECT COUNT(*) FROM platform.processed_message " +
            "WHERE consumer_name = ? AND message_id = ?";

    public static final String INSERT_PROCESSED_MESSAGE_IF_ABSENT =
            "INSERT INTO platform.processed_message (consumer_name, message_id, processed_at, expires_at) " +
            "VALUES (?, ?, ?, ?) " +
            "ON CONFLICT (consumer_name, message_id) DO NOTHING";

    public static final String DELETE_EXPIRED_PROCESSED_MESSAGES =
            "DELETE FROM platform.processed_message " +
            "WHERE expires_at <= ?";
}

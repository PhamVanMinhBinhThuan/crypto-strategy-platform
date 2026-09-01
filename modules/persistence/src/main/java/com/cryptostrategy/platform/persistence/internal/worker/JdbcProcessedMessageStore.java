package com.cryptostrategy.platform.persistence.internal.worker;

import com.cryptostrategy.platform.persistence.api.worker.ProcessedMessageStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

public final class JdbcProcessedMessageStore implements ProcessedMessageStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProcessedMessageStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
    }

    @Override
    public boolean isProcessed(String consumerName, String messageId) {
        Objects.requireNonNull(consumerName, "consumerName cannot be null");
        Objects.requireNonNull(messageId, "messageId cannot be null");
        Integer count = jdbcTemplate.queryForObject(
                WorkerSql.SELECT_IS_MESSAGE_PROCESSED,
                Integer.class,
                consumerName,
                messageId
        );
        return count != null && count > 0;
    }

    @Override
    public boolean insertIfAbsent(String consumerName, String messageId, Instant processedAt, Instant expiresAt) {
        Objects.requireNonNull(consumerName, "consumerName cannot be null");
        Objects.requireNonNull(messageId, "messageId cannot be null");
        Objects.requireNonNull(processedAt, "processedAt cannot be null");
        int rows = jdbcTemplate.update(
                WorkerSql.INSERT_PROCESSED_MESSAGE_IF_ABSENT,
                consumerName,
                messageId,
                Timestamp.from(processedAt),
                expiresAt != null ? Timestamp.from(expiresAt) : null
        );
        return rows > 0;
    }

    @Override
    public int purgeExpired(Instant threshold) {
        Objects.requireNonNull(threshold, "threshold cannot be null");
        return jdbcTemplate.update(
                WorkerSql.DELETE_EXPIRED_PROCESSED_MESSAGES,
                Timestamp.from(threshold)
        );
    }
}

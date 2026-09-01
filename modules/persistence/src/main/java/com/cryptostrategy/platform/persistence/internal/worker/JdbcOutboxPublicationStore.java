package com.cryptostrategy.platform.persistence.internal.worker;

import com.cryptostrategy.platform.persistence.api.worker.OutboxPublicationPort;
import com.cryptostrategy.platform.persistence.api.worker.OutboxRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class JdbcOutboxPublicationStore implements OutboxPublicationPort {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<OutboxRecord> rowMapper = (rs, rowNum) -> new OutboxRecord(
            rs.getString("outbox_event_id"),
            rs.getString("message_id"),
            rs.getString("aggregate_type"),
            rs.getString("aggregate_id"),
            rs.getString("event_type"),
            rs.getInt("event_version"),
            rs.getString("payload"),
            rs.getString("headers"),
            rs.getTimestamp("published_at") != null ? rs.getTimestamp("published_at").toInstant() : null,
            rs.getInt("publish_attempts"),
            rs.getString("last_error"),
            rs.getTimestamp("occurred_at").toInstant(),
            rs.getTimestamp("created_at").toInstant()
    );

    public JdbcOutboxPublicationStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
    }

    @Override
    public List<OutboxRecord> listUnpublishedBatch(int limit) {
        int boundedLimit = Math.max(1, limit);
        return jdbcTemplate.query(WorkerSql.SELECT_UNPUBLISHED_OUTBOX, rowMapper, boundedLimit);
    }

    @Override
    public void recordPublishSuccess(String outboxEventId, Instant publishedAt) {
        Objects.requireNonNull(outboxEventId, "outboxEventId cannot be null");
        Objects.requireNonNull(publishedAt, "publishedAt cannot be null");
        jdbcTemplate.update(WorkerSql.UPDATE_OUTBOX_SUCCESS, Timestamp.from(publishedAt), outboxEventId);
    }

    @Override
    public void recordPublishFailure(String outboxEventId, String lastError, Instant attemptedAt) {
        Objects.requireNonNull(outboxEventId, "outboxEventId cannot be null");
        jdbcTemplate.update(WorkerSql.UPDATE_OUTBOX_FAILURE, lastError, outboxEventId);
    }

    @Override
    public void markSuppressed(String outboxEventId, String reason) {
        Objects.requireNonNull(outboxEventId, "outboxEventId cannot be null");
        jdbcTemplate.update(
                "UPDATE platform.outbox_event SET last_error = ? WHERE outbox_event_id = ?",
                reason != null ? "SUPPRESSED: " + reason : "SUPPRESSED",
                outboxEventId
        );
    }
}

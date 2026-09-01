package com.cryptostrategy.platform.persistence.internal.experiment;

import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.experiment.api.port.out.OutboxStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

public class JdbcOutboxStore implements OutboxStore {

    private final JdbcTemplate jdbcTemplate;
    private final ExperimentJsonMapper jsonMapper;

    public JdbcOutboxStore(JdbcTemplate jdbcTemplate, ExperimentJsonMapper jsonMapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper cannot be null");
    }

    @Override
    public void insertOutboxEvent(OutboxEvent event) {
        jdbcTemplate.update(
                ExperimentSql.INSERT_OUTBOX_EVENT,
                event.outboxEventId(),
                event.messageId(),
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                event.eventVersion(),
                event.payloadJson(),
                jsonMapper.writeJson(event.headers()),
                Timestamp.from(event.occurredAt()),
                Timestamp.from(Instant.now())
        );
    }
}

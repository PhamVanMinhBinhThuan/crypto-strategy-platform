package com.cryptostrategy.platform.persistence.api;

import com.cryptostrategy.platform.persistence.api.worker.OutboxPublicationPort;
import com.cryptostrategy.platform.persistence.api.worker.ProcessedMessageStore;
import com.cryptostrategy.platform.persistence.internal.worker.JdbcOutboxPublicationStore;
import com.cryptostrategy.platform.persistence.internal.worker.JdbcProcessedMessageStore;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Objects;

public final class WorkerPersistenceFactory {

    private final JdbcTemplate jdbcTemplate;

    public WorkerPersistenceFactory(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource cannot be null");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public WorkerPersistenceFactory(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
    }

    public OutboxPublicationPort createOutboxPublicationPort() {
        return new JdbcOutboxPublicationStore(jdbcTemplate);
    }

    public ProcessedMessageStore createProcessedMessageStore() {
        return new JdbcProcessedMessageStore(jdbcTemplate);
    }
}

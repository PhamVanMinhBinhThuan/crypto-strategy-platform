package com.cryptostrategy.platform.worker.integration;

import com.cryptostrategy.platform.persistence.api.WorkerPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.worker.OutboxPublicationPort;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.engine.OutboxPublisherEngine;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxRedisPublishingIntegrationTest {

    private DataSource dataSource;
    private OutboxPublicationPort outboxPort;
    private RedisStreamPublisher streamPublisher;
    private WorkerProperties workerProperties;
    private OutboxPublisherEngine engine;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:worker_outbox_integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");
        this.dataSource = ds;

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS platform");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS platform.outbox_event (
                outbox_event_id VARCHAR(36) PRIMARY KEY,
                message_id VARCHAR(36) NOT NULL,
                aggregate_type VARCHAR(64) NOT NULL,
                aggregate_id VARCHAR(36) NOT NULL,
                event_type VARCHAR(64) NOT NULL,
                event_version INTEGER NOT NULL DEFAULT 1,
                payload TEXT NOT NULL,
                headers TEXT,
                published_at TIMESTAMP WITH TIME ZONE,
                publish_attempts INTEGER NOT NULL DEFAULT 0,
                last_error TEXT,
                occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """);
        jdbc.execute("DELETE FROM platform.outbox_event");

        WorkerPersistenceFactory factory = new WorkerPersistenceFactory(dataSource);
        this.outboxPort = factory.createOutboxPublicationPort();
        this.streamPublisher = mock(RedisStreamPublisher.class);
        this.workerProperties = new WorkerProperties(null, null, null, null, null, null, null, null);
        this.engine = new OutboxPublisherEngine(outboxPort, streamPublisher, workerProperties);
    }

    @Test
    void scansPendingOutboxRecordsFromDatabaseAndPublishesToRedisStream() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
            INSERT INTO platform.outbox_event (outbox_event_id, message_id, aggregate_type, aggregate_id, event_type, event_version, payload, occurred_at, created_at)
            VALUES ('evt-101', '01J7K8M9N0P1Q2R3S4T5A6V7W1', 'EXPERIMENT', '01J7K8M9N0P1Q2R3S4T5A6V7W1', 'EXPERIMENT_QUEUED', 1, '{"jobId":"01J7K8M9N0P1Q2R3S4T5A6V7W1"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);

        int published = engine.publishPendingOutboxBatch();

        assertThat(published).isEqualTo(1);
        verify(streamPublisher).publish(eq(workerProperties.streams().getBacktestJobsStream()), eq("01J7K8M9N0P1Q2R3S4T5A6V7W1"), any(), any());

        Integer pendingCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM platform.outbox_event WHERE published_at IS NULL", Integer.class
        );
        assertThat(pendingCount).isEqualTo(0);
    }
}

package com.cryptostrategy.platform.persistence.experiment;

import com.cryptostrategy.platform.persistence.api.WorkerPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.worker.OutboxPublicationPort;
import com.cryptostrategy.platform.persistence.api.worker.OutboxRecord;
import com.cryptostrategy.platform.persistence.api.worker.ProcessedMessageStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerOutboxPublicationIntegrationTest {

    private JdbcTemplate jdbcTemplate;
    private OutboxPublicationPort outboxPort;
    private ProcessedMessageStore processedStore;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(System.getenv("DATABASE_URL"));
        dataSource.setUsername(System.getenv("DATABASE_USERNAME"));
        dataSource.setPassword(System.getenv("DATABASE_PASSWORD"));
        dataSource.setDriverClassName("org.postgresql.Driver");

        WorkerPersistenceFactory factory = new WorkerPersistenceFactory(dataSource);
        outboxPort = factory.createOutboxPublicationPort();
        processedStore = factory.createProcessedMessageStore();
        jdbcTemplate = new JdbcTemplate(dataSource);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @RepeatedTest(2)
    void duplicateTolerantOutboxPublishingAndProcessedMessageIdempotency() {
        String eventId = com.cryptostrategy.platform.experiment.api.ExperimentId.generate().value();
        String messageId = com.cryptostrategy.platform.domain.api.identity.Ulids.generate();
        transaction.executeWithoutResult(status -> {
            status.setRollbackOnly();
            verifyPublicationAndIdempotency(eventId, messageId);
        });
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM platform.outbox_event WHERE outbox_event_id = ?",
                Integer.class, eventId)).isZero();
        assertThat(processedStore.isProcessed("worker-group-1", messageId)).isFalse();
    }

    private void verifyPublicationAndIdempotency(String eventId, String messageId) {
        Instant now = Instant.now();

        // 1. Insert an outbox event
        jdbcTemplate.update(
                "INSERT INTO platform.outbox_event (outbox_event_id, message_id, aggregate_type, aggregate_id, event_type, event_version, payload, occurred_at, created_at) " +
                "VALUES (?, ?, 'EXPERIMENT', ?, 'EXPERIMENT_QUEUED', 1, '{}'::jsonb, ?, ?)",
                eventId, messageId, messageId, Timestamp.from(now), Timestamp.from(now)
        );

        // 2. Fetch unpublished batch
        List<OutboxRecord> batch = outboxPort.listUnpublishedBatch(100);
        assertThat(batch.stream().anyMatch(r -> r.outboxEventId().equals(eventId))).isTrue();

        // 3. Mark success
        outboxPort.recordPublishSuccess(eventId, Instant.now());

        // 4. Verify no longer in unpublished batch
        List<OutboxRecord> batchAfter = outboxPort.listUnpublishedBatch(100);
        assertThat(batchAfter.stream().noneMatch(r -> r.outboxEventId().equals(eventId))).isTrue();

        // 5. Dual-layer ProcessedMessageStore idempotency
        String consumer = "worker-group-1";
        assertThat(processedStore.isProcessed(consumer, messageId)).isFalse();

        boolean firstInsert = processedStore.insertIfAbsent(consumer, messageId, now, now.plusSeconds(3600));
        assertThat(firstInsert).isTrue();
        assertThat(processedStore.isProcessed(consumer, messageId)).isTrue();

        // Duplicate insert must return false without exception
        boolean duplicateInsert = processedStore.insertIfAbsent(consumer, messageId, now, now.plusSeconds(3600));
        assertThat(duplicateInsert).isFalse();
    }
}

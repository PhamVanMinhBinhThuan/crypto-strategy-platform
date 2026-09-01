package com.cryptostrategy.platform.persistence.internal.worker;

import com.cryptostrategy.platform.persistence.api.worker.OutboxRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublicationStoreTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcOutboxPublicationStore store;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        store = new JdbcOutboxPublicationStore(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUnpublishedBatchQueriesSqlWithLimit() {
        OutboxRecord record = new OutboxRecord(
                "evt-1", "msg-1", "EXPERIMENT", "exp-1", "EXPERIMENT_QUEUED", 1,
                "{}", null, null, 0, null, Instant.now(), Instant.now()
        );
        when(jdbcTemplate.query(eq(WorkerSql.SELECT_UNPUBLISHED_OUTBOX), any(RowMapper.class), eq(25)))
                .thenReturn(List.of(record));

        List<OutboxRecord> result = store.listUnpublishedBatch(25);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).outboxEventId()).isEqualTo("evt-1");
    }

    @Test
    void recordPublishSuccessUpdatesTimestamp() {
        Instant now = Instant.now();
        store.recordPublishSuccess("evt-1", now);
        verify(jdbcTemplate).update(eq(WorkerSql.UPDATE_OUTBOX_SUCCESS), any(), eq("evt-1"));
    }

    @Test
    void recordPublishFailureIncrementsAttempts() {
        Instant now = Instant.now();
        store.recordPublishFailure("evt-1", "Connection timeout", now);
        verify(jdbcTemplate).update(eq(WorkerSql.UPDATE_OUTBOX_FAILURE), eq("Connection timeout"), eq("evt-1"));
    }
}

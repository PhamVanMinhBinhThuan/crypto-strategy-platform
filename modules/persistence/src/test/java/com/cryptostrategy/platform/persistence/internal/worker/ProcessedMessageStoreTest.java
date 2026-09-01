package com.cryptostrategy.platform.persistence.internal.worker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessedMessageStoreTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcProcessedMessageStore store;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        store = new JdbcProcessedMessageStore(jdbcTemplate);
    }

    @Test
    void isProcessedReturnsTrueWhenCountGreaterThanZero() {
        when(jdbcTemplate.queryForObject(eq(WorkerSql.SELECT_IS_MESSAGE_PROCESSED), eq(Integer.class), eq("consumer-1"), eq("msg-1")))
                .thenReturn(1);

        assertThat(store.isProcessed("consumer-1", "msg-1")).isTrue();
    }

    @Test
    void isProcessedReturnsFalseWhenCountIsZero() {
        when(jdbcTemplate.queryForObject(eq(WorkerSql.SELECT_IS_MESSAGE_PROCESSED), eq(Integer.class), eq("consumer-1"), eq("msg-1")))
                .thenReturn(0);

        assertThat(store.isProcessed("consumer-1", "msg-1")).isFalse();
    }

    @Test
    void insertIfAbsentReturnsTrueWhenRowInserted() {
        when(jdbcTemplate.update(eq(WorkerSql.INSERT_PROCESSED_MESSAGE_IF_ABSENT), eq("consumer-1"), eq("msg-1"), any(Timestamp.class), any()))
                .thenReturn(1);

        boolean inserted = store.insertIfAbsent("consumer-1", "msg-1", Instant.now(), Instant.now().plusSeconds(3600));
        assertThat(inserted).isTrue();
    }

    @Test
    void insertIfAbsentReturnsFalseWhenConflict() {
        when(jdbcTemplate.update(eq(WorkerSql.INSERT_PROCESSED_MESSAGE_IF_ABSENT), eq("consumer-1"), eq("msg-1"), any(Timestamp.class), any()))
                .thenReturn(0);

        boolean inserted = store.insertIfAbsent("consumer-1", "msg-1", Instant.now(), Instant.now().plusSeconds(3600));
        assertThat(inserted).isFalse();
    }

    @Test
    void purgeExpiredDeletesMessagesOlderThanThreshold() {
        Instant threshold = Instant.now();
        when(jdbcTemplate.update(eq(WorkerSql.DELETE_EXPIRED_PROCESSED_MESSAGES), any(Timestamp.class)))
                .thenReturn(5);

        int deleted = store.purgeExpired(threshold);
        assertThat(deleted).isEqualTo(5);
        verify(jdbcTemplate).update(eq(WorkerSql.DELETE_EXPIRED_PROCESSED_MESSAGES), any(Timestamp.class));
    }
}

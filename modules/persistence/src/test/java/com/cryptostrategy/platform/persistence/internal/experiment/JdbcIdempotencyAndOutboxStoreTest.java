package com.cryptostrategy.platform.persistence.internal.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcIdempotencyAndOutboxStoreTest {

    @Test
    @DisplayName("Idempotency SQL enforces atomic claim and completed updates")
    void idempotencySql() {
        assertThat(ExperimentSql.INSERT_IDEMPOTENCY_CLAIM).contains("ON CONFLICT (user_id, scope, idempotency_key) DO NOTHING");
        assertThat(ExperimentSql.COMPLETE_IDEMPOTENCY_RECORD).contains("SET state = 'COMPLETED'");
    }

    @Test
    @DisplayName("Outbox SQL inserts into platform.outbox_event")
    void outboxSql() {
        assertThat(ExperimentSql.INSERT_OUTBOX_EVENT).contains("INSERT INTO platform.outbox_event");
    }
}

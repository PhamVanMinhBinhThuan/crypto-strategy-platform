package com.cryptostrategy.platform.worker.engine;

import com.cryptostrategy.platform.persistence.api.worker.OutboxPublicationPort;
import com.cryptostrategy.platform.persistence.api.worker.OutboxRecord;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherEngineTest {

    private OutboxPublicationPort outboxPort;
    private RedisStreamPublisher streamPublisher;
    private WorkerProperties workerProperties;
    private OutboxPublisherEngine engine;

    @BeforeEach
    void setUp() {
        outboxPort = mock(OutboxPublicationPort.class);
        streamPublisher = mock(RedisStreamPublisher.class);
        workerProperties = new WorkerProperties(null, null, null, null, null, null, null, null);
        engine = new OutboxPublisherEngine(outboxPort, streamPublisher, workerProperties);
    }

    @Test
    void publishesOutboxBatchAndRecordsSuccess() {
        Instant now = Instant.now();
        OutboxRecord record = new OutboxRecord(
                "evt-1", "01J7K8M9N0P1Q2R3S4T5A6V7W1", "EXPERIMENT", "exp-1",
                "EXPERIMENT_QUEUED", 1, "{}", null, null, 0, null, now, now
        );
        when(outboxPort.listUnpublishedBatch(workerProperties.reconciliation().outboxBatchSize()))
                .thenReturn(List.of(record));

        int published = engine.publishPendingOutboxBatch();

        assertThat(published).isEqualTo(1);
        verify(streamPublisher).publish(eq(workerProperties.streams().getBacktestJobsStream()), eq("01J7K8M9N0P1Q2R3S4T5A6V7W1"), eq("{}"), any());
        verify(outboxPort).recordPublishSuccess(eq("evt-1"), any());
    }

    @Test
    void recordsFailureWhenRedisPublishFails() {
        Instant now = Instant.now();
        OutboxRecord record = new OutboxRecord(
                "evt-1", "01J7K8M9N0P1Q2R3S4T5A6V7W1", "EXPERIMENT", "exp-1",
                "EXPERIMENT_QUEUED", 1, "{}", null, null, 0, null, now, now
        );
        when(outboxPort.listUnpublishedBatch(workerProperties.reconciliation().outboxBatchSize()))
                .thenReturn(List.of(record));
        doThrow(new RuntimeException("Redis connection error")).when(streamPublisher).publish(anyString(), anyString(), anyString(), any());

        int published = engine.publishPendingOutboxBatch();

        assertThat(published).isEqualTo(0);
        verify(outboxPort).recordPublishFailure(eq("evt-1"), eq("Redis connection error"), any());
    }
}

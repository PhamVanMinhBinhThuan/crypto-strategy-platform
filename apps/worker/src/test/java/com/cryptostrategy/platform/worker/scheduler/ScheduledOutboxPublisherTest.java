package com.cryptostrategy.platform.worker.scheduler;

import com.cryptostrategy.platform.worker.engine.OutboxPublisherEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledOutboxPublisherTest {

    private OutboxPublisherEngine outboxPublisherEngine;
    private ScheduledOutboxPublisher scheduledPublisher;

    @BeforeEach
    void setUp() {
        outboxPublisherEngine = mock(OutboxPublisherEngine.class);
        scheduledPublisher = new ScheduledOutboxPublisher(outboxPublisherEngine);
    }

    @Test
    void scheduleOutboxPublishDelegatesToEngine() {
        when(outboxPublisherEngine.publishPendingOutboxBatch()).thenReturn(5);
        scheduledPublisher.scheduleOutboxPublish();
        verify(outboxPublisherEngine).publishPendingOutboxBatch();
    }
}

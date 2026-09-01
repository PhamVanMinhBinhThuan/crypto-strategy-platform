package com.cryptostrategy.platform.experiment.api.port.out;

import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;

public interface OutboxStore {
    void insertOutboxEvent(OutboxEvent event);
}

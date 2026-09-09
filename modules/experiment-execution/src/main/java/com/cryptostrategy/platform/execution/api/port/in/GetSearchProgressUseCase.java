package com.cryptostrategy.platform.execution.api.port.in;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import java.util.Optional;

/** Read-only projection that keeps Search implementation types behind execution ownership. */
public interface GetSearchProgressUseCase {
    Optional<SearchProgressSnapshot> findByExperimentId(ExperimentId experimentId);

    record SearchProgressSnapshot(String terminalReason) {}
}

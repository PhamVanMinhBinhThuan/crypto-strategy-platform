package com.cryptostrategy.platform.execution.api.port.in;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;

import java.util.Objects;

public record SearchCoordinationCommand(
        JobId searchJobId,
        ExperimentId experimentId,
        int concurrencyHint,
        int topKTarget,
        String correlationId) {
    public SearchCoordinationCommand {
        Objects.requireNonNull(searchJobId, "searchJobId");
        Objects.requireNonNull(experimentId, "experimentId");
        correlationId = requireText(correlationId, "correlationId");
        if (concurrencyHint < 1 || topKTarget < 1) {
            throw new IllegalArgumentException("coordination bounds must be positive");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}

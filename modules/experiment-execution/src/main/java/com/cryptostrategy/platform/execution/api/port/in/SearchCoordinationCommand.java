package com.cryptostrategy.platform.execution.api.port.in;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;

import java.util.Objects;

public record SearchCoordinationCommand(
        JobId searchJobId,
        ExperimentId experimentId,
        int concurrencyHint,
        int globalInFlightLimit,
        int topKTarget,
        String correlationId) {
    public SearchCoordinationCommand {
        Objects.requireNonNull(searchJobId, "searchJobId");
        Objects.requireNonNull(experimentId, "experimentId");
        correlationId = requireText(correlationId, "correlationId");
        if (concurrencyHint < 1 || globalInFlightLimit < 1 || topKTarget < 1) {
            throw new IllegalArgumentException("coordination bounds must be positive");
        }
    }

    public SearchCoordinationCommand(JobId searchJobId, ExperimentId experimentId,
            int concurrencyHint, int topKTarget, String correlationId) {
        this(searchJobId, experimentId, concurrencyHint, Integer.MAX_VALUE, topKTarget, correlationId);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}

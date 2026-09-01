package com.cryptostrategy.platform.experiment.api;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Experiment(
        ExperimentId experimentId,
        UUID ownerUserId,
        String name,
        ExperimentStatus status,
        ExperimentId derivedFromExperimentId,
        ExperimentId reproducesExperimentId,
        Instant startedAt,
        Instant completedAt,
        String failureCode,
        String failureMessage,
        Instant createdAt
) {
    public Experiment {
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public static Experiment create(
            ExperimentId experimentId,
            UUID ownerUserId,
            String name,
            ExperimentId derivedFromExperimentId,
            ExperimentId reproducesExperimentId,
            Instant createdAt
    ) {
        return new Experiment(
                experimentId,
                ownerUserId,
                name,
                ExperimentStatus.CREATED,
                derivedFromExperimentId,
                reproducesExperimentId,
                null,
                null,
                null,
                null,
                createdAt
        );
    }
}

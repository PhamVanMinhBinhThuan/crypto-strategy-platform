package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.error.ExperimentValidationException;
import com.cryptostrategy.platform.experiment.api.error.InvalidStateTransitionException;

import java.time.Instant;
import java.util.Objects;

public class ExperimentAggregate {

    private Experiment experiment;
    private ExperimentManifest manifest;

    public ExperimentAggregate(Experiment experiment, ExperimentManifest manifest) {
        this.experiment = Objects.requireNonNull(experiment, "experiment cannot be null");
        this.manifest = Objects.requireNonNull(manifest, "manifest cannot be null");
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public ExperimentManifest getManifest() {
        return manifest;
    }

    public void updateDraftManifest(ExperimentManifest updatedManifest) {
        if (!experiment.status().isMutable()) {
            throw new InvalidStateTransitionException(
                    "Cannot update manifest in immutable status: " + experiment.status()
            );
        }
        this.manifest = Objects.requireNonNull(updatedManifest, "updatedManifest cannot be null");
    }

    public void freezeAndQueue(String calculatedFingerprint, Instant queuedAt) {
        if (experiment.status() != ExperimentStatus.CREATED) {
            throw new InvalidStateTransitionException(
                    "Cannot freeze experiment in status: " + experiment.status() + " (must be CREATED)"
            );
        }
        if (calculatedFingerprint == null || calculatedFingerprint.isBlank()) {
            throw new ExperimentValidationException("Calculated fingerprint cannot be null or blank");
        }

        this.manifest = manifest.withFingerprint(calculatedFingerprint);
        this.experiment = new Experiment(
                experiment.experimentId(),
                experiment.ownerUserId(),
                experiment.name(),
                ExperimentStatus.QUEUED,
                experiment.derivedFromExperimentId(),
                experiment.reproducesExperimentId(),
                queuedAt,
                null,
                null,
                null,
                experiment.createdAt()
        );
    }

    public void requestStop(Instant stoppedAt) {
        if (experiment.status() != ExperimentStatus.RUNNING && experiment.status() != ExperimentStatus.QUEUED) {
            throw new InvalidStateTransitionException(
                    "Cannot request stop for experiment in status: " + experiment.status()
            );
        }
        this.experiment = new Experiment(
                experiment.experimentId(),
                experiment.ownerUserId(),
                experiment.name(),
                ExperimentStatus.STOP_REQUESTED,
                experiment.derivedFromExperimentId(),
                experiment.reproducesExperimentId(),
                experiment.startedAt(),
                null,
                null,
                null,
                experiment.createdAt()
        );
    }

    public void markStopped(Instant stoppedAt) {
        this.experiment = new Experiment(
                experiment.experimentId(),
                experiment.ownerUserId(),
                experiment.name(),
                ExperimentStatus.STOPPED,
                experiment.derivedFromExperimentId(),
                experiment.reproducesExperimentId(),
                experiment.startedAt(),
                stoppedAt,
                null,
                null,
                experiment.createdAt()
        );
    }

    public void markCompleted(Instant completedAt) {
        this.experiment = new Experiment(
                experiment.experimentId(),
                experiment.ownerUserId(),
                experiment.name(),
                ExperimentStatus.COMPLETED,
                experiment.derivedFromExperimentId(),
                experiment.reproducesExperimentId(),
                experiment.startedAt(),
                completedAt,
                null,
                null,
                experiment.createdAt()
        );
    }

    public void markFailed(String failureCode, String failureMessage, Instant failedAt) {
        this.experiment = new Experiment(
                experiment.experimentId(),
                experiment.ownerUserId(),
                experiment.name(),
                ExperimentStatus.FAILED,
                experiment.derivedFromExperimentId(),
                experiment.reproducesExperimentId(),
                experiment.startedAt(),
                failedAt,
                failureCode,
                failureMessage,
                experiment.createdAt()
        );
    }
}

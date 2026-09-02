package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.domain.api.identity.Ulids;
import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.error.ExperimentValidationException;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.experiment.api.port.in.CompleteStoppedExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.CreateCandidateUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.CreateExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.FreezeExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ReproduceExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.StopExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import com.cryptostrategy.platform.experiment.api.job.Job;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ExperimentApplicationService implements
        CreateExperimentUseCase,
        FreezeExperimentUseCase,
        GetExperimentUseCase,
        StopExperimentUseCase,
        CompleteStoppedExperimentUseCase,
        ReproduceExperimentUseCase,
        CreateCandidateUseCase,
        ListCandidatesUseCase {

    private final ExperimentStore experimentStore;
    private final CanonicalFingerprintCalculator fingerprintCalculator;

    public ExperimentApplicationService(
            ExperimentStore experimentStore,
            CanonicalFingerprintCalculator fingerprintCalculator
    ) {
        this.experimentStore = Objects.requireNonNull(experimentStore, "experimentStore cannot be null");
        this.fingerprintCalculator = Objects.requireNonNull(fingerprintCalculator, "fingerprintCalculator cannot be null");
    }

    @Override
    public Experiment createExperiment(
            UUID ownerUserId,
            String name,
            ExperimentManifest draftManifest,
            ExperimentId derivedFromExperimentId,
            ExperimentId reproducesExperimentId
    ) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(draftManifest, "draftManifest cannot be null");

        Instant now = Instant.now();
        ExperimentId experimentId = draftManifest.experimentId() != null ?
                draftManifest.experimentId() : ExperimentId.generate();

        Experiment experiment = Experiment.create(
                experimentId,
                ownerUserId,
                name,
                derivedFromExperimentId,
                reproducesExperimentId,
                now
        );

        ExperimentManifest manifest = new ExperimentManifest(
                experimentId,
                draftManifest.manifestVersion(),
                draftManifest.datasetProvenance(),
                draftManifest.strategyProvenance(),
                draftManifest.backtestConfig(),
                draftManifest.searchConfig(),
                draftManifest.evaluationConfig(),
                draftManifest.sentimentConfig(),
                draftManifest.softwareVersion(),
                draftManifest.gitCommit(),
                null,
                now
        );

        experimentStore.insertExperiment(ownerUserId, experiment, manifest);
        return experiment;
    }

    @Override
    public Experiment freezeAndQueue(UUID ownerUserId, ExperimentId experimentId) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");

        Experiment experiment = experimentStore.findExperimentById(ownerUserId, experimentId)
                .orElseThrow(() -> new ResourceInaccessibleException("Experiment not found or inaccessible"));
        ExperimentManifest manifest = experimentStore.findManifestByExperimentId(ownerUserId, experimentId)
                .orElseThrow(() -> new ResourceInaccessibleException("Manifest not found or inaccessible"));

        ExperimentAggregate aggregate = new ExperimentAggregate(experiment, manifest);
        String fingerprint = fingerprintCalculator.calculate(manifest);
        Instant now = Instant.now();

        aggregate.freezeAndQueue(fingerprint, now);

        OutboxEvent outboxEvent = OutboxEvents.experimentQueued(aggregate.getExperiment(), aggregate.getManifest(), now);
        experimentStore.freezeAndQueueExperiment(ownerUserId, experimentId, fingerprint, now, outboxEvent);

        return aggregate.getExperiment();
    }

    @Override
    public Optional<Experiment> getExperiment(UUID ownerUserId, ExperimentId experimentId) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        return experimentStore.findExperimentById(ownerUserId, experimentId);
    }

    @Override
    public Optional<ExperimentManifest> getManifest(UUID ownerUserId, ExperimentId experimentId) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        return experimentStore.findManifestByExperimentId(ownerUserId, experimentId);
    }

    @Override
    public Experiment stopExperiment(UUID ownerUserId, ExperimentId experimentId) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");

        Experiment experiment = experimentStore.findExperimentById(ownerUserId, experimentId)
                .orElseThrow(() -> new ResourceInaccessibleException("Experiment not found or inaccessible"));
        ExperimentManifest manifest = experimentStore.findManifestByExperimentId(ownerUserId, experimentId)
                .orElseThrow(() -> new ResourceInaccessibleException("Manifest not found or inaccessible"));

        ExperimentAggregate aggregate = new ExperimentAggregate(experiment, manifest);
        Instant now = Instant.now();
        aggregate.requestStop(now);

        OutboxEvent outboxEvent = OutboxEvents.experimentStopRequested(aggregate.getExperiment(), now);
        experimentStore.stopExperimentWithOutbox(ownerUserId, experimentId, outboxEvent, now);

        return aggregate.getExperiment();
    }

    @Override
    public CandidateDefinition createCandidate(
            UUID ownerUserId,
            ExperimentId experimentId,
            int generationIndex,
            Map<String, Object> definition,
            Map<String, Object> generatorState,
            String fingerprint
    ) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");

        Experiment experiment = experimentStore.findExperimentById(ownerUserId, experimentId)
                .orElseThrow(() -> new ResourceInaccessibleException("Experiment not found or inaccessible"));

        if (experiment.status() == ExperimentStatus.CREATED) {
            throw new ExperimentValidationException("Cannot create candidates for un-frozen experiment in CREATED status");
        }

        CandidateId candidateId = CandidateId.generate();
        Instant now = Instant.now();
        CandidateDefinition candidate = new CandidateDefinition(
                candidateId,
                experimentId,
                generationIndex,
                definition,
                generatorState,
                fingerprint,
                now
        );

        experimentStore.insertCandidate(ownerUserId, candidate);
        return candidate;
    }

    @Override
    public List<CandidateDefinition> listCandidates(UUID ownerUserId, ExperimentId experimentId) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        return experimentStore.listCandidatesByExperimentId(ownerUserId, experimentId);
    }

    @Override
    public List<CandidateDefinition> listCandidates(
            UUID ownerUserId,
            ExperimentId experimentId,
            int afterGenerationIndex,
            String afterCandidateId,
            int limit) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(afterCandidateId, "afterCandidateId cannot be null");
        if (afterGenerationIndex < -1 || limit < 1 || limit > 101) {
            throw new IllegalArgumentException("Candidate page boundary is invalid");
        }
        return experimentStore.listCandidatesPage(
                ownerUserId,
                experimentId,
                afterGenerationIndex,
                afterCandidateId,
                limit);
    }

    @Override
    public Optional<CandidateDefinition> getCandidate(
            UUID ownerUserId, ExperimentId experimentId, CandidateId candidateId) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(candidateId, "candidateId cannot be null");

        if (experimentStore.findExperimentById(ownerUserId, experimentId).isEmpty()) {
            return Optional.empty();
        }
        return experimentStore.findCandidateById(ownerUserId, candidateId)
                .filter(candidate -> candidate.experimentId().equals(experimentId));
    }

    @Override
    public Experiment reproduceExperiment(UUID ownerUserId, ExperimentId sourceExperimentId, String newName) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(sourceExperimentId, "sourceExperimentId cannot be null");

        Experiment sourceExp = experimentStore.findExperimentById(ownerUserId, sourceExperimentId)
                .orElseThrow(() -> new ResourceInaccessibleException("Source experiment not found or inaccessible"));
        ExperimentManifest sourceManifest = experimentStore.findManifestByExperimentId(ownerUserId, sourceExperimentId)
                .orElseThrow(() -> new ResourceInaccessibleException("Source manifest not found or inaccessible"));

        Instant now = Instant.now();
        ExperimentId newExperimentId = ExperimentId.generate();
        String name = newName != null && !newName.isBlank() ? newName : "Reproduction of " + sourceExp.name();

        Experiment newExperiment = Experiment.create(
                newExperimentId,
                ownerUserId,
                name,
                sourceExp.derivedFromExperimentId(),
                sourceExperimentId, // reproducesExperimentId
                now
        );

        ExperimentManifest newManifest = new ExperimentManifest(
                newExperimentId,
                sourceManifest.manifestVersion(),
                sourceManifest.datasetProvenance(),
                sourceManifest.strategyProvenance(),
                sourceManifest.backtestConfig(),
                sourceManifest.searchConfig(),
                sourceManifest.evaluationConfig(),
                sourceManifest.sentimentConfig(),
                sourceManifest.softwareVersion(),
                sourceManifest.gitCommit(),
                null,
                now
        );

        experimentStore.insertExperiment(ownerUserId, newExperiment, newManifest);
        return newExperiment;
    }

    @Override
    public boolean completeIfEligible(ExperimentId experimentId) {
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        UUID ownerUserId = experimentStore.findOwnerUserIdByExperimentId(experimentId)
                .orElse(null);
        if (ownerUserId == null) {
            return false;
        }
        Experiment experiment = experimentStore.findExperimentById(ownerUserId, experimentId)
                .orElse(null);
        if (experiment == null) {
            return false;
        }
        if (experiment.status() == ExperimentStatus.STOPPED) {
            return true;
        }
        if (experiment.status() != ExperimentStatus.STOP_REQUESTED) {
            return false;
        }
        List<Job> jobs = experimentStore.listAllJobsByExperimentId(experimentId);
        boolean allTerminal = jobs.stream().allMatch(j -> j.status().isTerminal());
        if (!allTerminal) {
            return false;
        }
        Instant now = Instant.now();
        experimentStore.updateExperimentStatus(ownerUserId, experimentId, ExperimentStatus.STOPPED, now);
        return true;
    }
}

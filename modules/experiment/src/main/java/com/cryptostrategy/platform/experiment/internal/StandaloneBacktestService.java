package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.backtest.BacktestId;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktest;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktestAcceptance;
import com.cryptostrategy.platform.experiment.api.backtest.StartStandaloneBacktestCommand;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.port.in.StartStandaloneBacktestUseCase;
import com.cryptostrategy.platform.experiment.api.port.out.StandaloneBacktestStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Builds and atomically accepts one immutable single-run Experiment graph. */
public final class StandaloneBacktestService implements StartStandaloneBacktestUseCase {
    private static final Duration RECEIPT_LIFETIME = Duration.ofHours(24);

    private final StandaloneBacktestStore store;
    private final CanonicalFingerprintCalculator fingerprints;
    private final Clock clock;

    public StandaloneBacktestService(
            StandaloneBacktestStore store,
            CanonicalFingerprintCalculator fingerprints,
            Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.fingerprints = Objects.requireNonNull(fingerprints, "fingerprints");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public StandaloneBacktestAcceptance startStandaloneBacktest(
            UUID ownerUserId, StartStandaloneBacktestCommand command) {
        Objects.requireNonNull(ownerUserId, "ownerUserId cannot be null");
        Objects.requireNonNull(command, "command cannot be null");

        Instant now = clock.instant();
        BacktestId backtestId = BacktestId.generate();
        ExperimentId experimentId = ExperimentId.generate();
        CandidateId candidateId = CandidateId.generate();
        JobId jobId = JobId.generate();

        Experiment draftExperiment = Experiment.create(
                experimentId,
                ownerUserId,
                "Standalone Backtest " + backtestId.value(),
                null,
                null,
                now);
        ExperimentManifest draftManifest = new ExperimentManifest(
                experimentId,
                "experiment-manifest-v1",
                command.datasetProvenance(),
                command.strategyProvenance(),
                command.backtestConfig(),
                Map.of("mode", "SINGLE_BACKTEST"),
                command.evaluationConfig(),
                null,
                command.softwareVersion(),
                command.gitCommit(),
                null,
                now);
        ExperimentAggregate aggregate = new ExperimentAggregate(draftExperiment, draftManifest);
        aggregate.freezeAndQueue(fingerprints.calculate(draftManifest), now);

        CandidateDefinition candidate = new CandidateDefinition(
                candidateId,
                experimentId,
                0,
                Map.of(),
                Map.of("mode", "SINGLE_BACKTEST"),
                command.strategyProvenance().strategyFingerprint(),
                now);
        Job job = Job.createBacktestJob(
                jobId, experimentId, candidateId, command.correlationId(), now);
        StandaloneBacktest backtest = new StandaloneBacktest(
                backtestId, experimentId, candidateId, jobId, now);

        return store.accept(
                ownerUserId,
                StartStandaloneBacktestUseCase.OPERATION,
                command.idempotencyKey(),
                command.canonicalRequestHash(),
                now.plus(RECEIPT_LIFETIME),
                backtest,
                aggregate.getExperiment(),
                aggregate.getManifest(),
                candidate,
                job,
                OutboxEvents.jobQueued(job, now));
    }
}

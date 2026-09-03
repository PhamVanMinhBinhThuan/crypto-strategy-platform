package com.cryptostrategy.platform.persistence.experiment;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.ExecutionAttempt;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import com.cryptostrategy.platform.experiment.api.job.JobType;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.persistence.api.ExperimentPersistenceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AttemptCancellationCompletionRaceIntegrationTest {

    private ExperimentPersistenceFactory factory;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(System.getenv("DATABASE_URL"));
        dataSource.setUsername(System.getenv("DATABASE_USERNAME"));
        dataSource.setPassword(System.getenv("DATABASE_PASSWORD"));
        dataSource.setDriverClassName("org.postgresql.Driver");
        factory = new ExperimentPersistenceFactory(dataSource);
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void cancellationVsSuccessRaceEnsuresNoOverwriteOfTerminalState() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        ExperimentIntegrationFixture.seedManifestReferences(
                jdbc, ownerUserId, "01J7K8M9N0P1Q2R3S4T5A6V7W2");
        ExperimentId experimentId = ExperimentId.generate();
        CandidateId candidateId = CandidateId.generate();
        JobId jobId = JobId.generate();
        Instant now = Instant.now();

        var experimentStore = factory.createExperimentStore();
        var jobStore = factory.createJobStore();
        var attemptStore = factory.createExecutionAttemptStore();

        // 1. Setup Experiment, Manifest, Candidate, Job
        Experiment experiment = Experiment.create(experimentId, ownerUserId, "Cancel Vs Success Race", null, null, now);
        ExperimentManifest manifest = new ExperimentManifest(
                experimentId, "1",
                new DatasetProvenanceSnapshot(new DatasetVersionId("01J7K8M9N0P1Q2R3S4T5A6V7W2"), "v1", "hash", "binance", "BTCUSDT", "1m", "norm-v1", now.minusSeconds(3600), now, 100),
                StrategyProvenanceSnapshot.single(new com.cryptostrategy.platform.strategy.api.model.StrategyReference(new com.cryptostrategy.platform.strategy.api.model.StrategyVersionId("01J7K8M9N0P1Q2R3S4T5A6V7W2"), new StrategyPluginId("momentum"), new com.cryptostrategy.platform.strategy.api.model.SemanticVersion(1, 0, 0)), com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet.empty(), java.util.Optional.empty(), "strategy-v1:sha256:0000000000000000000000000000000000000000000000000000000000000000"),
                Map.of("capital", 10000), Map.of(), Map.of(), null, "1.0", "commit-1", "fingerprint-1", now
        );
        experimentStore.insertExperiment(ownerUserId, experiment, manifest);

        CandidateDefinition candidate = new CandidateDefinition(
                candidateId, experimentId, 0, Map.of("period", 14), null, "cand-fingerprint", now
        );
        experimentStore.insertCandidate(ownerUserId, candidate);

        Job job = new Job(
                jobId, experimentId, candidateId, JobType.BACKTEST, JobStatus.QUEUED, "corr-1",
                1, 0, 0, null, now, null, null, null, null, null, now, now
        );
        jobStore.insertJob(ownerUserId, job, null);

        ExecutionAttempt attempt = attemptStore.startNextAttempt(ownerUserId, jobId, "worker-1", now);
        AttemptId attemptId = attempt.attemptId();

        // 2. Compete: Worker (finalizeSuccess) vs User Cancellation (finalizeCancelled)
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Future<Boolean> worker = executor.submit(() -> {
            latch.await();
            return attemptStore.finalizeAttemptSuccess(ownerUserId, jobId, attemptId, Instant.now());
        });

        Future<Boolean> cancel = executor.submit(() -> {
            latch.await();
            return attemptStore.finalizeAttemptCancelled(ownerUserId, jobId, attemptId, Instant.now());
        });

        latch.countDown();
        boolean workerResult = worker.get(5, TimeUnit.SECONDS);
        boolean cancelResult = cancel.get(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one should succeed
        assertThat(workerResult ^ cancelResult).isTrue();

        Job finalJob = jobStore.findJobById(ownerUserId, jobId).orElseThrow();
        assertThat(finalJob.status() == JobStatus.SUCCEEDED || finalJob.status() == JobStatus.CANCELLED).isTrue();
    }
}

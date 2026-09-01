package com.cryptostrategy.platform.persistence.experiment;

import com.cryptostrategy.platform.backtesting.api.model.BacktestAssumptions;
import com.cryptostrategy.platform.backtesting.api.model.BacktestProvenance;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.backtesting.api.model.EquityCurveSummary;
import com.cryptostrategy.platform.backtesting.api.model.Money;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResultId;
import com.cryptostrategy.platform.evaluation.api.model.MetricVersion;
import com.cryptostrategy.platform.evaluation.api.model.RankingVersion;
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
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenance;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetVersionId;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyPluginId;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenance;
import com.cryptostrategy.platform.persistence.api.BacktestingPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.EvaluationPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.ExperimentPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.LeaderboardPersistenceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BacktestAtomicCompletionIntegrationTest {

    private ExperimentPersistenceFactory expFactory;
    private BacktestingPersistenceFactory btFactory;
    private EvaluationPersistenceFactory evalFactory;
    private LeaderboardPersistenceFactory lbFactory;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(System.getenv("DATABASE_URL"));
        dataSource.setUsername(System.getenv("DATABASE_USERNAME"));
        dataSource.setPassword(System.getenv("DATABASE_PASSWORD"));
        dataSource.setDriverClassName("org.postgresql.Driver");

        expFactory = new ExperimentPersistenceFactory(dataSource);
        btFactory = new BacktestingPersistenceFactory(dataSource);
        evalFactory = new EvaluationPersistenceFactory(dataSource);
        lbFactory = new LeaderboardPersistenceFactory(dataSource);
    }

    @Test
    void atomicCompletionSucceedsAndPersistsLineageAndEvaluation() {
        UUID ownerUserId = UUID.randomUUID();
        ExperimentId experimentId = ExperimentId.generate();
        CandidateId candidateId = CandidateId.generate();
        JobId jobId = JobId.generate();
        Instant now = Instant.now();

        var experimentStore = expFactory.createExperimentStore();
        var jobStore = expFactory.createJobStore();
        var attemptStore = expFactory.createExecutionAttemptStore();
        var btResultStore = btFactory.createBacktestResultStore();
        var evalResultStore = evalFactory.createEvaluationResultStore();
        var lbStore = lbFactory.createLeaderboardStore();

        // 1. Setup Experiment, Candidate, Job
        Experiment experiment = Experiment.create(experimentId, ownerUserId, "Atomic Test", null, null, now);
        ExperimentManifest manifest = new ExperimentManifest(
                experimentId, 1,
                new DatasetProvenance(new DatasetVersionId("01J7K8M9N0P1Q2R3S4T5A6V7W3"), "BTCUSDT", "1m", now.minusSeconds(3600), now, 100, "hash"),
                StrategyProvenance.single(new StrategyPluginId("momentum"), 1, Map.of("period", 14), null),
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

        // 2. Start attempt
        ExecutionAttempt attempt = attemptStore.startNextAttempt(ownerUserId, jobId, "worker-1", now);
        AttemptId attemptId = attempt.attemptId();

        // 3. Finalize attempt to SUCCEEDED
        boolean finalized = attemptStore.finalizeAttemptSuccess(ownerUserId, jobId, attemptId, now);
        assertThat(finalized).isTrue();

        // 4. Save BacktestResult with SUCCEEDED attempt lineage
        BacktestResultId resultId = BacktestResultId.generate();
        BacktestAssumptions assumptions = BacktestAssumptions.mvp(BigDecimal.valueOf(10000), BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.0005));
        EquityCurveSummary summary = new EquityCurveSummary(100L, Money.of(BigDecimal.valueOf(12000)), Money.of(BigDecimal.valueOf(9500)), 10L, 50L, "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

        BacktestResult result = new BacktestResult(
                resultId, experimentId, candidateId, jobId, attemptId,
                new BacktestProvenance("fp", "ds-cs", "st-fp"),
                assumptions, Money.of(BigDecimal.valueOf(10000)),
                Money.of(BigDecimal.valueOf(12000)), Money.of(BigDecimal.ZERO),
                List.of(), summary,
                "result-fp-1", now
        );
        BacktestResult savedResult = btResultStore.save(result);
        assertThat(savedResult.resultId()).isEqualTo(resultId);

        // 5. Save EvaluationResult
        EvaluationResultId evalId = EvaluationResultId.generate();
        EvaluationResult eval = new EvaluationResult(
                evalId, experimentId, resultId, new MetricVersion("metric-v1"), new RankingVersion("ranking-v1"),
                BigDecimal.valueOf(0.20), BigDecimal.valueOf(0.60), BigDecimal.valueOf(0.05), 10,
                BigDecimal.valueOf(0.85), true, "eval-fp-1", now
        );
        EvaluationResult savedEval = evalResultStore.save(eval);
        assertThat(savedEval.evaluationResultId()).isEqualTo(evalId);

        // 6. Verify listEvaluationsForExperiment
        List<EvaluationResult> evals = lbStore.listEvaluationsForExperiment(experimentId, 10);
        assertThat(evals).hasSize(1);
        assertThat(evals.get(0).evaluationResultId()).isEqualTo(evalId);
    }
}

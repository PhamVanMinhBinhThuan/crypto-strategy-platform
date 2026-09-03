package com.cryptostrategy.platform.persistence.internal.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cryptostrategy.platform.execution.api.port.in.StartSearchReproductionUseCase;
import com.cryptostrategy.platform.execution.api.port.out.SearchReproductionGateway;
import com.cryptostrategy.platform.execution.internal.SearchReproductionApplicationService;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.persistence.internal.execution.JdbcSearchExperimentTransaction;
import com.cryptostrategy.platform.persistence.internal.search.FiniteSearchExperimentIntegrationTest;
import com.cryptostrategy.platform.persistence.internal.search.SearchAllocationConcurrencyIntegrationTest;
import com.cryptostrategy.platform.persistence.internal.search.JdbcSearchReproductionVerificationGateway;
import com.cryptostrategy.platform.persistence.internal.execution.JdbcExecutionEvidenceReader;
import com.cryptostrategy.platform.execution.internal.SearchReproductionVerificationCoordinator;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class SearchReproductionIntegrationTest {
    @Test
    void atomicCopyPreservesSourceAndCreatesPendingVerificationWithReplayConflict() {
        var dataSource = SearchAllocationConcurrencyIntegrationTest.dataSource();
        new TransactionTemplate(new DataSourceTransactionManager(dataSource)).executeWithoutResult(tx -> {
            tx.setRollbackOnly();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            Source source = seedCompletedSource(jdbc, dataSource);
            var service = new SearchReproductionApplicationService(new JdbcSearchExperimentTransaction(dataSource));

            var accepted = service.start(command("reproduce-key", "same-hash"));
            var replay = service.start(command("reproduce-key", "same-hash"));

            assertThat(replay.replay()).isTrue();
            assertThat(replay.experimentId()).isEqualTo(accepted.experimentId());
            assertThat(jdbc.queryForObject("select count(*) from experiment.candidate_definition where experiment_id=?",
                    Integer.class, accepted.experimentId().value())).isEqualTo(1);
            assertThat(jdbc.queryForObject("select definition::text from experiment.candidate_definition where experiment_id=?",
                    String.class, accepted.experimentId().value())).isEqualTo(source.definition());
            assertThat(jdbc.queryForObject("select status from search.reproduction_verification where reproduction_experiment_id=?",
                    String.class, accepted.experimentId().value())).isEqualTo("PENDING");
            assertThat(jdbc.queryForObject("select definition::text from experiment.candidate_definition where candidate_id=?",
                    String.class, source.candidateId())).isEqualTo(source.definition());
            assertThatThrownBy(() -> service.start(command("reproduce-key", "different-hash")))
                    .isInstanceOf(IdempotencyConflictException.class);

            String reproducedCandidate = jdbc.queryForObject(
                    "select candidate_id from experiment.candidate_definition where experiment_id=?",
                    String.class, accepted.experimentId().value());
            String reproducedJob = jdbc.queryForObject(
                    "select job_id from experiment.job where experiment_id=? and job_type='BACKTEST'",
                    String.class, accepted.experimentId().value());
            seedReproducedEvidence(jdbc, accepted.experimentId().value(), reproducedCandidate, reproducedJob);
            jdbc.update("update experiment.experiment set status='COMPLETED',started_at=?,completed_at=? where experiment_id=?",
                    java.sql.Timestamp.from(SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(11)),
                    java.sql.Timestamp.from(SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(12)),
                    accepted.experimentId().value());
            var verification = new JdbcSearchReproductionVerificationGateway(dataSource);
            var evidenceReader = new JdbcExecutionEvidenceReader(jdbc);
            assertThat(evidenceReader.load(SearchAllocationConcurrencyIntegrationTest.OWNER,
                    new ExperimentId(SearchAllocationConcurrencyIntegrationTest.EXPERIMENT))).isNotNull();
            assertThat(evidenceReader.load(SearchAllocationConcurrencyIntegrationTest.OWNER,
                    accepted.experimentId())).isNotNull();
            var coordinator = new SearchReproductionVerificationCoordinator(verification,
                    evidenceReader, Clock.fixed(
                    SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(14), ZoneOffset.UTC));
            assertThat(coordinator.verify(accepted.experimentId()))
                    .isEqualTo(SearchReproductionVerificationCoordinator.Result.MATCHED);
            assertThat(verification.claimReady(accepted.experimentId(),
                    SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(15))).isEmpty();
            assertThat(jdbc.queryForObject("select status from search.reproduction_verification where reproduction_experiment_id=?",
                    String.class, accepted.experimentId().value())).isEqualTo("MATCHED");
        });
    }

    @Test
    void copyFailureRollsBackTheWholeTargetGraph() {
        var dataSource = SearchAllocationConcurrencyIntegrationTest.dataSource();
        new TransactionTemplate(new DataSourceTransactionManager(dataSource)).executeWithoutResult(tx -> {
            tx.setRollbackOnly();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            seedCompletedSource(jdbc, dataSource);
            var gateway = new JdbcSearchExperimentTransaction(dataSource);
            var source = gateway.loadSource(SearchAllocationConcurrencyIntegrationTest.OWNER,
                    new ExperimentId(SearchAllocationConcurrencyIntegrationTest.EXPERIMENT)).orElseThrow();
            var service = new SearchReproductionApplicationService(new SearchReproductionGateway() {
                @Override public java.util.Optional<SourceSnapshot> loadSource(java.util.UUID owner, ExperimentId id) {
                    return java.util.Optional.of(new SourceSnapshot(id, source.status(), true,
                            java.util.List.of("63000000000000000000000099")));
                }
                @Override public Result create(CreateCommand command) { return gateway.create(command); }
            });

            assertThatThrownBy(() -> service.start(command("rollback-key", "rollback-hash")))
                    .isInstanceOf(Exception.class);
        });
        assertThat(new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from experiment.experiment where reproduces_experiment_id=?",
                Integer.class, SearchAllocationConcurrencyIntegrationTest.EXPERIMENT)).isZero();
    }

    private static Source seedCompletedSource(JdbcTemplate jdbc, javax.sql.DataSource dataSource) {
        SearchAllocationConcurrencyIntegrationTest.seed(jdbc);
        FiniteSearchExperimentIntegrationTest.seedManifest(jdbc);
        var allocation = SearchAllocationConcurrencyIntegrationTest.allocation(
                new JdbcSearchExperimentTransaction(dataSource),
                new com.cryptostrategy.platform.search.api.port.out.SearchRunClaim(
                        SearchAllocationConcurrencyIntegrationTest.initialRun().start(
                                SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(1)), 1),
                "A", new java.util.concurrent.CountDownLatch(0), new java.util.concurrent.CountDownLatch(0));
        try { allocation.call(); } catch (Exception failure) { throw new RuntimeException(failure); }
        String candidate = "6200000000000000000000001A";
        String job = "6200000000000000000000002A";
        FiniteSearchExperimentIntegrationTest.seedEvaluationAndLeaderboard(jdbc, candidate, job);
        jdbc.update("update experiment.experiment set status='COMPLETED',started_at=?,completed_at=? where experiment_id=?",
                java.sql.Timestamp.from(SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(1)),
                java.sql.Timestamp.from(SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(5)),
                SearchAllocationConcurrencyIntegrationTest.EXPERIMENT);
        return new Source(candidate, jdbc.queryForObject(
                "select definition::text from experiment.candidate_definition where candidate_id=?", String.class, candidate));
    }

    private static StartSearchReproductionUseCase.Command command(String key, String hash) {
        Instant now = SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(10);
        return new StartSearchReproductionUseCase.Command(SearchAllocationConcurrencyIntegrationTest.OWNER,
                new ExperimentId(SearchAllocationConcurrencyIntegrationTest.EXPERIMENT), "Reproduction", key, hash,
                "reproduce-f010", now, now.plusSeconds(3600));
    }

    private static void seedReproducedEvidence(JdbcTemplate jdbc, String experiment, String candidate, String job) {
        Instant completed = SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(12);
        jdbc.update("update experiment.job set status='SUCCEEDED',completed_work=1,finished_at=? where job_id=?",
                java.sql.Timestamp.from(completed), job);
        jdbc.update("insert into experiment.execution_attempt(attempt_id,job_id,candidate_id,attempt_no,status,started_at,finished_at) values ('62000000000000000000000084',?,?,1,'SUCCEEDED',?,?)",
                job, candidate, java.sql.Timestamp.from(completed.minusSeconds(1)), java.sql.Timestamp.from(completed));
        jdbc.update("insert into experiment.backtest_result(backtest_result_id,candidate_id,successful_attempt_id,initial_capital,final_capital,result_fingerprint,completed_at,experiment_id,job_id,manifest_fingerprint,dataset_fingerprint,strategy_fingerprint,assumptions_version,assumptions_json,total_fees,equity_point_count,equity_peak,equity_trough,equity_peak_sequence,equity_trough_sequence,equity_curve_fingerprint) values ('62000000000000000000000085',?,'62000000000000000000000084',1000,1010,'result',?,?,?,?,?,'strategy','backtest-assumptions-v1',?::jsonb,1,2,1010,1000,0,1,?)",
                candidate, java.sql.Timestamp.from(completed), experiment, job, "manifest-f010",
                "sha256:" + "7".repeat(64), FiniteSearchExperimentIntegrationTest.assumptions(),
                "sha256:" + "8".repeat(64));
        jdbc.update("insert into experiment.evaluation_result(evaluation_result_id,backtest_result_id,metric_version,ranking_version,total_return,win_rate,maximum_drawdown,number_of_trades,overall_score,evaluated_at,experiment_id,return_score,win_rate_score,drawdown_score,leaderboard_eligible,evaluation_fingerprint) values ('62000000000000000000000086','62000000000000000000000085','metric-v1','ranking-v1',0.01,0.5,0.1,1,0.5,?,?,0.5,0.5,0.5,true,?)",
                java.sql.Timestamp.from(completed), experiment, "sha256:" + "9".repeat(64));
        jdbc.update("insert into experiment.leaderboard_revision(leaderboard_revision_id,experiment_id,revision_no,top_k,ranking_version,revision_fingerprint) values ('62000000000000000000000087',?,1,1,'ranking-v1',?)",
                experiment, "sha256:" + "a".repeat(64));
        jdbc.update("insert into experiment.leaderboard_entry(leaderboard_revision_id,rank,evaluation_result_id,score,maximum_drawdown,evaluation_fingerprint,experiment_id) values ('62000000000000000000000087',1,'62000000000000000000000086',0.5,0.1,?,?)",
                "sha256:" + "9".repeat(64), experiment);
    }

    private record Source(String candidateId, String definition) {}
}

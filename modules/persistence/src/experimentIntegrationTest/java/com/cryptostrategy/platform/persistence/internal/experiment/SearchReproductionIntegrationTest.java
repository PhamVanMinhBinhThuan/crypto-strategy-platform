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
            SourceGraphSnapshot sourceBefore = snapshotSourceGraph(jdbc);
            var service = new SearchReproductionApplicationService(new JdbcSearchExperimentTransaction(dataSource));

            var accepted = service.start(command("reproduce-key", "same-hash"));
            var replay = service.start(command("reproduce-key", "same-hash"));

            assertThat(replay.replay()).isTrue();
            assertThat(replay.experimentId()).isEqualTo(accepted.experimentId());
            assertThat(accepted.experimentId().value())
                    .isNotEqualTo(SearchAllocationConcurrencyIntegrationTest.EXPERIMENT);
            assertThat(jdbc.queryForObject(
                    "select reproduces_experiment_id from experiment.experiment where experiment_id=?",
                    String.class, accepted.experimentId().value()))
                    .isEqualTo(SearchAllocationConcurrencyIntegrationTest.EXPERIMENT);
            assertThat(jdbc.queryForObject("select count(*) from experiment.candidate_definition where experiment_id=?",
                    Integer.class, accepted.experimentId().value())).isEqualTo(1);
            assertThat(jdbc.queryForObject("select definition::text from experiment.candidate_definition where experiment_id=?",
                    String.class, accepted.experimentId().value())).isEqualTo(source.definition());
            assertThat(jdbc.queryForObject("select status from search.reproduction_verification where reproduction_experiment_id=?",
                    String.class, accepted.experimentId().value())).isEqualTo("PENDING");
            assertThat(jdbc.queryForObject("select definition::text from experiment.candidate_definition where candidate_id=?",
                    String.class, source.candidateId())).isEqualTo(source.definition());
            assertThat(snapshotSourceGraph(jdbc)).isEqualTo(sourceBefore);
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
            assertThat(snapshotSourceGraph(jdbc)).isEqualTo(sourceBefore);
            String verificationId = jdbc.queryForObject(
                    "select verification_id from search.reproduction_verification where reproduction_experiment_id=?",
                    String.class, accepted.experimentId().value());
            System.out.printf(
                    "F014_REPRODUCTION_EVIDENCE sourceExperimentId=%s reproductionExperimentId=%s "
                            + "verificationId=%s originalResultId=%s reproducedResultId=%s "
                            + "originalEvaluationId=%s reproducedEvaluationId=%s "
                            + "originalLeaderboardRevisionId=%s reproducedLeaderboardRevisionId=%s verdict=MATCHED%n",
                    SearchAllocationConcurrencyIntegrationTest.EXPERIMENT,
                    accepted.experimentId().value(),
                    verificationId,
                    "62000000000000000000000075",
                    "62000000000000000000000085",
                    "62000000000000000000000076",
                    "62000000000000000000000086",
                    "62000000000000000000000077",
                    "62000000000000000000000087");
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

    @Test
    void missingManifestRollsBackTheWholeTargetGraph() {
        assertIncompleteSourceRollsBack(false);
    }

    @Test
    void missingSearchRunRollsBackTheWholeTargetGraph() {
        assertIncompleteSourceRollsBack(true);
    }

    private static void assertIncompleteSourceRollsBack(boolean removeSearchRun) {
        var dataSource = SearchAllocationConcurrencyIntegrationTest.dataSource();
        new TransactionTemplate(new DataSourceTransactionManager(dataSource)).executeWithoutResult(tx -> {
            tx.setRollbackOnly();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            seedCompletedSource(jdbc, dataSource);
            if (removeSearchRun) {
                jdbc.update("delete from search.coordination_decision where search_run_id in (select search_run_id from search.search_run where experiment_id=?)",
                        SearchAllocationConcurrencyIntegrationTest.EXPERIMENT);
                assertThat(jdbc.update("delete from search.search_run where experiment_id=?",
                        SearchAllocationConcurrencyIntegrationTest.EXPERIMENT)).isEqualTo(1);
            } else {
                assertThat(jdbc.update("delete from experiment.experiment_manifest where experiment_id=?",
                        SearchAllocationConcurrencyIntegrationTest.EXPERIMENT)).isEqualTo(1);
            }
            var service = new SearchReproductionApplicationService(new JdbcSearchExperimentTransaction(dataSource));

            assertThatThrownBy(() -> service.start(command("incomplete-key", "incomplete-hash")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("source graph is incomplete");
        });
        JdbcTemplate verify = new JdbcTemplate(dataSource);
        assertThat(verify.queryForObject(
                "select count(*) from experiment.experiment where reproduces_experiment_id=?",
                Integer.class, SearchAllocationConcurrencyIntegrationTest.EXPERIMENT)).isZero();
        assertThat(verify.queryForObject(
                "select count(*) from platform.idempotency_record where scope='REPRODUCE_SEARCH' and idempotency_key='incomplete-key'",
                Integer.class)).isZero();
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
                "62000000000000000000000090", now, now.plusSeconds(3600));
    }

    private static void seedReproducedEvidence(JdbcTemplate jdbc, String experiment, String candidate, String job) {
        Instant completed = SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(12);
        Instant sourceEvidenceCompleted = SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(4);
        jdbc.update("update experiment.job set status='SUCCEEDED',completed_work=1,finished_at=? where job_id=?",
                java.sql.Timestamp.from(completed), job);
        jdbc.update("insert into experiment.execution_attempt(attempt_id,job_id,candidate_id,attempt_no,status,started_at,finished_at) values ('62000000000000000000000084',?,?,1,'SUCCEEDED',?,?)",
                job, candidate, java.sql.Timestamp.from(completed.minusSeconds(1)), java.sql.Timestamp.from(completed));
        jdbc.update("insert into experiment.backtest_result(backtest_result_id,candidate_id,successful_attempt_id,initial_capital,final_capital,result_fingerprint,completed_at,experiment_id,job_id,manifest_fingerprint,dataset_fingerprint,strategy_fingerprint,assumptions_version,assumptions_json,total_fees,equity_point_count,equity_peak,equity_trough,equity_peak_sequence,equity_trough_sequence,equity_curve_fingerprint) values ('62000000000000000000000085',?,'62000000000000000000000084',1000,1010,'result',?,?,?,?,?,'strategy','backtest-assumptions-v1',?::jsonb,1,2,1010,1000,0,1,?)",
                candidate, java.sql.Timestamp.from(completed), experiment, job, "manifest-f010",
                "sha256:" + "7".repeat(64), FiniteSearchExperimentIntegrationTest.assumptions(),
                "sha256:" + "8".repeat(64));
        jdbc.update("insert into experiment.trade(trade_id,backtest_result_id,sequence_no,side,entry_time,exit_time,entry_price,exit_price,quantity,entry_fee,exit_fee,fee,profit_loss,post_trade_cash,exit_reason) values ('62000000000000000000000088','62000000000000000000000085',0,'BUY',?,?,?,?,?,?,?,?,?,?,?)",
                java.sql.Timestamp.from(sourceEvidenceCompleted.minusSeconds(3)),
                java.sql.Timestamp.from(sourceEvidenceCompleted.minusSeconds(2)), 100, 110, 1,
                new java.math.BigDecimal("0.1"), new java.math.BigDecimal("0.1"),
                new java.math.BigDecimal("0.2"), new java.math.BigDecimal("9.8"),
                new java.math.BigDecimal("1009.8"), "STRATEGY_SELL");
        jdbc.update("insert into experiment.evaluation_result(evaluation_result_id,backtest_result_id,metric_version,ranking_version,total_return,win_rate,maximum_drawdown,number_of_trades,overall_score,evaluated_at,experiment_id,return_score,win_rate_score,drawdown_score,leaderboard_eligible,evaluation_fingerprint) values ('62000000000000000000000086','62000000000000000000000085','metric-v1','ranking-v1',0.01,0.5,0.1,1,0.5,?,?,0.5,0.5,0.5,true,?)",
                java.sql.Timestamp.from(completed), experiment, "sha256:" + "9".repeat(64));
        jdbc.update("insert into experiment.leaderboard_revision(leaderboard_revision_id,experiment_id,revision_no,top_k,ranking_version,revision_fingerprint) values ('62000000000000000000000087',?,1,1,'ranking-v1',?)",
                experiment, "sha256:" + "a".repeat(64));
        jdbc.update("insert into experiment.leaderboard_entry(leaderboard_revision_id,rank,evaluation_result_id,score,maximum_drawdown,evaluation_fingerprint,experiment_id) values ('62000000000000000000000087',1,'62000000000000000000000086',0.5,0.1,?,?)",
                "sha256:" + "9".repeat(64), experiment);
    }

    private static SourceGraphSnapshot snapshotSourceGraph(JdbcTemplate jdbc) {
        String experiment = SearchAllocationConcurrencyIntegrationTest.EXPERIMENT;
        return new SourceGraphSnapshot(
                singleJson(jdbc, "select to_jsonb(value)::text from experiment.experiment_manifest value where experiment_id=?", experiment),
                singleJson(jdbc, "select to_jsonb(value)::text from experiment.candidate_definition value where experiment_id=?", experiment),
                singleJson(jdbc, "select to_jsonb(value)::text from experiment.backtest_result value where experiment_id=?", experiment),
                jdbc.queryForList("select to_jsonb(value)::text from experiment.trade value where backtest_result_id in (select backtest_result_id from experiment.backtest_result where experiment_id=?) order by sequence_no", String.class, experiment),
                singleJson(jdbc, "select to_jsonb(value)::text from experiment.evaluation_result value where experiment_id=?", experiment),
                singleJson(jdbc, "select to_jsonb(value)::text from experiment.leaderboard_revision value where experiment_id=?", experiment));
    }

    private static String singleJson(JdbcTemplate jdbc, String sql, String experimentId) {
        return jdbc.queryForObject(sql, String.class, experimentId);
    }

    private record Source(String candidateId, String definition) {}

    private record SourceGraphSnapshot(String manifest, String candidate, String acceptedResult,
            java.util.List<String> trades, String evaluation, String leaderboardRevision) {
        private SourceGraphSnapshot {
            trades = java.util.List.copyOf(trades);
        }
    }
}

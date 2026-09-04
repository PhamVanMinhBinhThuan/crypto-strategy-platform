package com.cryptostrategy.platform.persistence.internal.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.execution.api.port.in.SearchCoordinationCommand;
import com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase;
import com.cryptostrategy.platform.execution.internal.SearchCandidateAllocationService;
import com.cryptostrategy.platform.execution.internal.TrustedSearchCoordinationService;
import com.cryptostrategy.platform.persistence.internal.execution.JdbcSearchExperimentTransaction;
import com.cryptostrategy.platform.search.api.SearchModuleFactory;
import com.cryptostrategy.platform.search.api.model.SearchRunStatus;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Finite durable pipeline: generator -> Candidate/Job -> Evaluation/Leaderboard -> terminal. */
public class FiniteSearchExperimentIntegrationTest {
    private static final String DATASET = "62000000000000000000000070";
    private static final String BASE = "62000000000000000000000071";
    private static final String QUOTE = "62000000000000000000000072";
    private static final String PAIR = "62000000000000000000000073";

    @Test
    void finiteSearchReachesAuthoritativeLeaderboardAndTerminalState() {
        var dataSource = SearchAllocationConcurrencyIntegrationTest.dataSource();
        new TransactionTemplate(new DataSourceTransactionManager(dataSource)).executeWithoutResult(tx -> {
            tx.setRollbackOnly();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            SearchAllocationConcurrencyIntegrationTest.seed(jdbc);
            seedManifest(jdbc);
            jdbc.update("update search.search_run set maximum_candidates=1,max_in_flight=1 where search_run_id=?",
                    SearchAllocationConcurrencyIntegrationTest.RUN);
            jdbc.update("update experiment.job set total_work=1 where job_id=?",
                    SearchAllocationConcurrencyIntegrationTest.SEARCH_JOB);

            JdbcSearchRunStore runStore = new JdbcSearchRunStore(jdbc);
            var search = SearchModuleFactory.baseline(runStore);
            var allocator = new SearchCandidateAllocationService(runStore, search.generation(),
                    new JdbcSearchAllocationContextGateway(jdbc),
                    new JdbcSearchExperimentTransaction(dataSource),
                    Clock.fixed(SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(2), ZoneOffset.UTC),
                    new com.fasterxml.jackson.databind.ObjectMapper());
            var allocated = allocator.fillAvailableSlots(new SearchCoordinationCommand(
                    new com.cryptostrategy.platform.experiment.api.job.JobId(SearchAllocationConcurrencyIntegrationTest.SEARCH_JOB),
                    new com.cryptostrategy.platform.experiment.api.ExperimentId(SearchAllocationConcurrencyIntegrationTest.EXPERIMENT),
                    1, 1, "finite-f010"));

            assertThat(allocated.allocatedWork()).isEqualTo(1);
            String candidate = jdbc.queryForObject("select candidate_id from experiment.candidate_definition where experiment_id=?",
                    String.class, SearchAllocationConcurrencyIntegrationTest.EXPERIMENT);
            String job = jdbc.queryForObject("select job_id from experiment.job where experiment_id=? and job_type='BACKTEST'",
                    String.class, SearchAllocationConcurrencyIntegrationTest.EXPERIMENT);
            seedEvaluationAndLeaderboard(jdbc, candidate, job);

            var trusted = new TrustedSearchCoordinationService(
                    new JdbcTrustedSearchCoordinationGateway(dataSource), Clock.systemUTC());
            var outcome = trusted.reconcileCompletion(new TrustedSearchCoordinationUseCase.CompletionTrigger(
                    "62000000000000000000000079",
                    new com.cryptostrategy.platform.experiment.api.ExperimentId(SearchAllocationConcurrencyIntegrationTest.EXPERIMENT),
                    new com.cryptostrategy.platform.experiment.api.CandidateId(candidate),
                    new com.cryptostrategy.platform.experiment.api.job.JobId(job),
                    SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(5), "finite-f010"));

            assertThat(outcome.status()).isEqualTo(SearchRunStatus.COMPLETED);
            assertThat(jdbc.queryForObject(
                    "select status from experiment.experiment where experiment_id=?",
                    String.class, SearchAllocationConcurrencyIntegrationTest.EXPERIMENT))
                    .isEqualTo("COMPLETED");
            assertThat(jdbc.queryForObject("select count(*) from experiment.leaderboard_entry le join experiment.leaderboard_revision lr using(leaderboard_revision_id) where lr.experiment_id=?",
                    Integer.class, SearchAllocationConcurrencyIntegrationTest.EXPERIMENT)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from platform.outbox_event where aggregate_id=? and event_type='BACKTEST_JOB'",
                    Integer.class, job)).isEqualTo(1);
        });
    }

    public static void seedManifest(JdbcTemplate jdbc) {
        jdbc.update("insert into market.asset(asset_id,symbol) values (?, 'F010BASE'),(?,'F010QUOTE')", BASE, QUOTE);
        jdbc.update("insert into market.trading_pair(trading_pair_id,base_asset_id,quote_asset_id,symbol) values (?,?,?,'F010BASEF010QUOTE')",
                PAIR, BASE, QUOTE);
        jdbc.update("insert into market.dataset_version(dataset_version_id,version,provider,trading_pair_id,timeframe,normalization_version,range_start,range_end,candle_count,checksum) values (?,'v1','fixture',?,'1h','v1',?,?,1,?)",
                DATASET, PAIR, Timestamp.from(SearchAllocationConcurrencyIntegrationTest.NOW.minusSeconds(3600)),
                Timestamp.from(SearchAllocationConcurrencyIntegrationTest.NOW), "sha256:" + "7".repeat(64));
        String config = "{\"searchSpace\":{\"period\":{\"type\":\"INTEGER\",\"options\":[\"5\",\"10\"]}}}";
        jdbc.update("insert into experiment.experiment_manifest(experiment_id,manifest_version,dataset_version_id,strategy_kind,strategy_ref_id,strategy_version,strategy_parameters,backtest_config,search_config,evaluation_config,software_version,git_commit,fingerprint,dataset_provenance,strategy_provenance) values (?,'v1',?,'SINGLE','momentum','1.0.0','{}','{}',?::jsonb,'{}','test','93eb912','manifest-f010','{}','{}')",
                SearchAllocationConcurrencyIntegrationTest.EXPERIMENT, DATASET, config);
    }

    public static void seedEvaluationAndLeaderboard(JdbcTemplate jdbc, String candidate, String job) {
        Instant completed = SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(4);
        jdbc.update("update experiment.job set status='SUCCEEDED',completed_work=1,finished_at=? where job_id=?",
                Timestamp.from(completed), job);
        jdbc.update("insert into experiment.execution_attempt(attempt_id,job_id,candidate_id,attempt_no,status,started_at,finished_at) values ('62000000000000000000000074',?,?,1,'SUCCEEDED',?,?)",
                job, candidate, Timestamp.from(completed.minusSeconds(1)), Timestamp.from(completed));
        jdbc.update("insert into experiment.backtest_result(backtest_result_id,candidate_id,successful_attempt_id,initial_capital,final_capital,result_fingerprint,completed_at,experiment_id,job_id,manifest_fingerprint,dataset_fingerprint,strategy_fingerprint,assumptions_version,assumptions_json,total_fees,equity_point_count,equity_peak,equity_trough,equity_peak_sequence,equity_trough_sequence,equity_curve_fingerprint) values ('62000000000000000000000075',?, '62000000000000000000000074',1000,1010,'result',?,?,?,?,?,'strategy','backtest-assumptions-v1',?::jsonb,1,2,1010,1000,0,1,?)",
                candidate, Timestamp.from(completed), SearchAllocationConcurrencyIntegrationTest.EXPERIMENT,
                job, "manifest-f010", "sha256:" + "7".repeat(64), assumptions(),
                "sha256:" + "8".repeat(64));
        jdbc.update("insert into experiment.trade(trade_id,backtest_result_id,sequence_no,side,entry_time,exit_time,entry_price,exit_price,quantity,entry_fee,exit_fee,fee,profit_loss,post_trade_cash,exit_reason) values ('62000000000000000000000078','62000000000000000000000075',0,'BUY',?,?,?,?,?,?,?,?,?,?,?)",
                Timestamp.from(completed.minusSeconds(3)), Timestamp.from(completed.minusSeconds(2)),
                100, 110, 1, new java.math.BigDecimal("0.1"), new java.math.BigDecimal("0.1"),
                new java.math.BigDecimal("0.2"), new java.math.BigDecimal("9.8"),
                new java.math.BigDecimal("1009.8"), "STRATEGY_SELL");
        jdbc.update("insert into experiment.evaluation_result(evaluation_result_id,backtest_result_id,metric_version,ranking_version,total_return,win_rate,maximum_drawdown,number_of_trades,overall_score,evaluated_at,experiment_id,return_score,win_rate_score,drawdown_score,leaderboard_eligible,evaluation_fingerprint) values ('62000000000000000000000076','62000000000000000000000075','metric-v1','ranking-v1',0.01,0.5,0.1,1,0.5,?,?,0.5,0.5,0.5,true,?)",
                Timestamp.from(completed), SearchAllocationConcurrencyIntegrationTest.EXPERIMENT,
                "sha256:" + "9".repeat(64));
        jdbc.update("insert into experiment.leaderboard_revision(leaderboard_revision_id,experiment_id,revision_no,top_k,ranking_version,revision_fingerprint) values ('62000000000000000000000077',?,1,1,'ranking-v1',?)",
                SearchAllocationConcurrencyIntegrationTest.EXPERIMENT, "sha256:" + "a".repeat(64));
        jdbc.update("insert into experiment.leaderboard_entry(leaderboard_revision_id,rank,evaluation_result_id,score,maximum_drawdown,evaluation_fingerprint,experiment_id) values ('62000000000000000000000077',1,'62000000000000000000000076',0.5,0.1,?,?)",
                "sha256:" + "9".repeat(64), SearchAllocationConcurrencyIntegrationTest.EXPERIMENT);
    }

    public static String assumptions() {
        return "{\"contractVersion\":\"backtest-assumptions-v1\",\"initialCapital\":{\"value\":1000},"
                + "\"feeRate\":0,\"slippageRate\":0,\"positionMode\":\"LONG_ONLY\","
                + "\"executionPriceRule\":\"NEXT_CANDLE_OPEN\",\"forceCloseAtEnd\":true,"
                + "\"roundingMode\":\"HALF_EVEN\"}";
    }
}

package com.cryptostrategy.platform.persistence.internal.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.execution.api.port.in.TrustedSearchCoordinationUseCase;
import com.cryptostrategy.platform.execution.internal.TrustedSearchCoordinationService;
import com.cryptostrategy.platform.persistence.internal.execution.JdbcSearchExperimentTransaction;
import com.cryptostrategy.platform.search.api.model.SearchRunStatus;
import com.cryptostrategy.platform.search.api.port.out.SearchRunClaim;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class SearchDeadlineIntegrationTest {
    private static final String CANDIDATE = "6200000000000000000000001A";
    private static final String BACKTEST_JOB = "6200000000000000000000002A";

    @Test
    void injectedUtcClockAndOnTimeAuthoritativeCompletionSurviveGatewayRestart() throws Exception {
        runScenario(false, SearchRunStatus.COMPLETED,
                TrustedSearchCoordinationUseCase.Decision.COMPLETE);
    }

    @Test
    void completionAfterFrozenDeadlineCannotWinAndRunStops() throws Exception {
        runScenario(true, SearchRunStatus.STOPPED,
                TrustedSearchCoordinationUseCase.Decision.STOP);
    }

    private static void runScenario(
            boolean late,
            SearchRunStatus expectedStatus,
            TrustedSearchCoordinationUseCase.Decision expectedDecision) throws Exception {
        var dataSource = SearchAllocationConcurrencyIntegrationTest.dataSource();
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transaction.executeWithoutResult(status -> {
            status.setRollbackOnly();
            var jdbc = new JdbcTemplate(dataSource);
            SearchAllocationConcurrencyIntegrationTest.seed(jdbc);
            try {
            var running = SearchAllocationConcurrencyIntegrationTest.initialRun()
                    .start(SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(1));
            var allocation = SearchAllocationConcurrencyIntegrationTest.allocation(
                    new JdbcSearchExperimentTransaction(dataSource),
                    new SearchRunClaim(running, running.version()), "A",
                    new CountDownLatch(0), new CountDownLatch(0)).call();
            assertThat(allocation.status().name()).isEqualTo("ALLOCATED");
            Instant completedAt = late ? running.deadlineAt().plusSeconds(1) : running.deadlineAt().minusSeconds(1);
            seedTerminalEvidence(jdbc, completedAt);

            // Tạo adapter/service mới mô phỏng process restart; deadline phải được reload từ DB.
            var gateway = new JdbcTrustedSearchCoordinationGateway(dataSource);
            var service = new TrustedSearchCoordinationService(
                    gateway, Clock.fixed(completedAt.plusSeconds(1), ZoneOffset.UTC));
            var outcome = service.reconcileCompletion(new TrustedSearchCoordinationUseCase.CompletionTrigger(
                    "6200000000000000000000009A",
                    new com.cryptostrategy.platform.experiment.api.ExperimentId(SearchAllocationConcurrencyIntegrationTest.EXPERIMENT),
                    new com.cryptostrategy.platform.experiment.api.CandidateId(CANDIDATE),
                    new com.cryptostrategy.platform.experiment.api.job.JobId(BACKTEST_JOB),
                    completedAt.plusSeconds(1), "correlation-f010"));

            assertThat(outcome.status()).isEqualTo(expectedStatus);
            assertThat(outcome.decision()).isEqualTo(expectedDecision);
            var durable = new JdbcTrustedSearchCoordinationGateway(dataSource)
                    .load(new com.cryptostrategy.platform.experiment.api.ExperimentId(
                            SearchAllocationConcurrencyIntegrationTest.EXPERIMENT)).orElseThrow();
            assertThat(durable.run().deadlineAt()).isEqualTo(running.deadlineAt());
            assertThat(durable.completedWork()).isEqualTo(1);
            assertThat(jdbc.queryForObject("select completed_work from experiment.job where job_id=?",
                    Integer.class, SearchAllocationConcurrencyIntegrationTest.SEARCH_JOB)).isEqualTo(1);
            } catch (Exception failure) {
                throw new RuntimeException(failure);
            }
        });
    }

    private static void seedTerminalEvidence(JdbcTemplate jdbc, Instant completedAt) {
        jdbc.update("update search.search_run set maximum_candidates=1 where search_run_id=?",
                SearchAllocationConcurrencyIntegrationTest.RUN);
        jdbc.update("update experiment.job set status='SUCCEEDED',completed_work=1,finished_at=? where job_id=?",
                Timestamp.from(completedAt), BACKTEST_JOB);
        jdbc.update("insert into experiment.execution_attempt(attempt_id,job_id,candidate_id,attempt_no,status,started_at,finished_at) "
                        + "values ('6200000000000000000000006A',?,?,1,'SUCCEEDED',?,?)",
                BACKTEST_JOB, CANDIDATE, Timestamp.from(completedAt.minusSeconds(1)), Timestamp.from(completedAt));
        jdbc.update("insert into experiment.backtest_result(backtest_result_id,candidate_id,successful_attempt_id,initial_capital,final_capital," 
                        + "result_fingerprint,completed_at,experiment_id,job_id,manifest_fingerprint,dataset_fingerprint,strategy_fingerprint," 
                        + "assumptions_version,assumptions_json,total_fees,equity_point_count,equity_peak,equity_trough,equity_peak_sequence," 
                        + "equity_trough_sequence,equity_curve_fingerprint) values ('6200000000000000000000007A',?,?,1000,1010,'result',?,?,?,?,?,?," 
                        + "'v1','{}',1,2,1010,1000,0,1,?)",
                CANDIDATE, "6200000000000000000000006A", Timestamp.from(completedAt),
                SearchAllocationConcurrencyIntegrationTest.EXPERIMENT, BACKTEST_JOB, "manifest", "dataset", "strategy",
                "sha256:" + "1".repeat(64));
        jdbc.update("insert into experiment.evaluation_result(evaluation_result_id,backtest_result_id,metric_version,ranking_version,total_return," 
                        + "win_rate,maximum_drawdown,number_of_trades,overall_score,evaluated_at,experiment_id,return_score,win_rate_score," 
                        + "drawdown_score,leaderboard_eligible,evaluation_fingerprint) values " 
                        + "('6200000000000000000000008A','6200000000000000000000007A','metrics-v1','ranking-v1',0.01,0.5,0.1,1,0.5,?,?,0.5,0.5,0.5,true,?)",
                Timestamp.from(completedAt), SearchAllocationConcurrencyIntegrationTest.EXPERIMENT,
                "sha256:" + "2".repeat(64));
    }

}

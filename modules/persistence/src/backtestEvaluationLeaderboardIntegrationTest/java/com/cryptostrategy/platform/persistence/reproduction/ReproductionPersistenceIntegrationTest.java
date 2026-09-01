package com.cryptostrategy.platform.persistence.reproduction;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptostrategy.platform.backtesting.api.model.*;
import com.cryptostrategy.platform.backtesting.internal.BacktestReproductionVerifier;
import com.cryptostrategy.platform.persistence.api.BacktestingPersistenceFactory;
import com.cryptostrategy.platform.persistence.support.F006DatabaseFixture;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

    @Test
    void linkedReproductionRunPersistsVerificationAndDetectsMismatch() {
        var source = F006DatabaseFixture.dataSource();
        F006DatabaseFixture.transaction(source).executeWithoutResult(status -> {
            status.setRollbackOnly(); 
            var jdbc = new JdbcTemplate(source);
            F006DatabaseFixture.seed(jdbc);

            // Seed Evaluation and Leaderboard for original
            var originalBacktest = F006DatabaseFixture.result();
            var bFactory = new BacktestingPersistenceFactory(source);
            bFactory.createResultStore().save(originalBacktest);

            jdbc.update("insert into experiment.evaluation_result(evaluation_result_id,experiment_id,backtest_result_id,metric_version,ranking_version,total_return,win_rate,maximum_drawdown,number_of_trades,return_score,win_rate_score,drawdown_score,overall_score,leaderboard_eligible,evaluation_fingerprint,evaluated_at) values ('6000000000000000000000000E', ?, ?, 'v1', 'v1', 0,0,0,0,0,0,0,0,true,'efp',now())", F006DatabaseFixture.EXPERIMENT, originalBacktest.resultId().value());
            jdbc.update("insert into experiment.leaderboard_revision(leaderboard_revision_id,experiment_id,metric_version,ranking_version,minimum_score,maximum_drawdown,top_k,generated_at) values ('6000000000000000000000000L', ?, 'v1', 'v1', 0, 0, 10, now())", F006DatabaseFixture.EXPERIMENT);

            var reproId = new com.cryptostrategy.platform.experiment.api.ExperimentId("70000000000000000000000001");
            jdbc.update("insert into experiment.experiment(experiment_id,owner_user_id,name,status) values (?,?,'repro','RUNNING')", reproId.value(), java.util.UUID.fromString("90000000-0000-4000-8000-000000000001"));
            
            var factory = new com.cryptostrategy.platform.persistence.api.ExperimentExecutionPersistenceFactory(source, new com.fasterxml.jackson.databind.ObjectMapper());
            var reader = factory.createEvidenceReader();
            var verifications = factory.createVerificationStore();

            com.cryptostrategy.platform.execution.api.port.in.ReproduceExperimentUseCase experiments = id -> new com.cryptostrategy.platform.execution.api.model.FrozenExperiment(reproId, java.util.UUID.fromString("90000000-0000-4000-8000-000000000001"), F006DatabaseFixture.EXPERIMENT, new com.cryptostrategy.platform.experiment.api.ExperimentId(F006DatabaseFixture.EXPERIMENT), Instant.now());
            
            // Reproduced evidence with same fingerprint but different IDs
            var reproBacktest = new BacktestResult(
                    new BacktestResultId("7000000000000000000000000R"), reproId, originalBacktest.candidateId(), originalBacktest.jobId(), originalBacktest.successfulAttemptId(),
                    originalBacktest.provenance(), originalBacktest.assumptions(), originalBacktest.initialCapital(), originalBacktest.finalCapital(), originalBacktest.totalFees(),
                    originalBacktest.trades().stream().map(t -> new Trade(new TradeId("8000000000000000000000000R"), new BacktestResultId("7000000000000000000000000R"), t.sequence(), t.side(), t.entryTime(), t.exitTime(), t.entryPrice(), t.exitPrice(), t.quantity(), t.entryFee(), t.exitFee(), t.totalFee(), t.realizedPnl(), t.postTradeCash(), t.exitReason())).toList(),
                    originalBacktest.equityCurveSummary(), originalBacktest.fingerprint(), originalBacktest.completedAt());
            bFactory.createResultStore().save(reproBacktest);
            
            jdbc.update("insert into experiment.evaluation_result(evaluation_result_id,experiment_id,backtest_result_id,metric_version,ranking_version,total_return,win_rate,maximum_drawdown,number_of_trades,return_score,win_rate_score,drawdown_score,overall_score,leaderboard_eligible,evaluation_fingerprint,evaluated_at) values ('7000000000000000000000000E', ?, ?, 'v1', 'v1', 0,0,0,0,0,0,0,0,true,'efp',now())", reproId.value(), reproBacktest.resultId().value());
            jdbc.update("insert into experiment.leaderboard_revision(leaderboard_revision_id,experiment_id,metric_version,ranking_version,minimum_score,maximum_drawdown,top_k,generated_at) values ('7000000000000000000000000L', ?, 'v1', 'v1', 0, 0, 10, now())", reproId.value());
            
            com.cryptostrategy.platform.execution.api.port.out.ReproductionExecutionRunner runner = e -> reader.load(e.ownerUserId(), e.experimentId());

            var service = new com.cryptostrategy.platform.execution.internal.ReproduceExperimentExecutionService(
                experiments, reader, runner, verifications, 
                new BacktestReproductionVerifier(), 
                new com.cryptostrategy.platform.execution.internal.EvaluationReproductionVerifier(), 
                new com.cryptostrategy.platform.execution.internal.LeaderboardReproductionVerifier());
            
            var verification = service.reproduce(reproId, java.util.UUID.fromString("90000000-0000-4000-8000-000000000001"));
            assertEquals(com.cryptostrategy.platform.execution.api.VerificationOutcome.MATCHED, verification.outcome());
            assertEquals(F006DatabaseFixture.EXPERIMENT, verification.sourceExperimentId().value());
            
            int count = jdbc.queryForObject("select count(*) from experiment.reproduction_verification where reproduction_experiment_id = ?", Integer.class, reproId.value());
            assertEquals(1, count);
        });
    }
}

package com.cryptostrategy.platform.persistence.support;

import com.cryptostrategy.platform.backtesting.api.model.BacktestAssumptions;
import com.cryptostrategy.platform.backtesting.api.model.BacktestProvenance;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.backtesting.api.model.EquityCurveSummary;
import com.cryptostrategy.platform.backtesting.api.model.ExitReason;
import com.cryptostrategy.platform.backtesting.api.model.Money;
import com.cryptostrategy.platform.backtesting.api.model.PositionSide;
import com.cryptostrategy.platform.backtesting.api.model.Quantity;
import com.cryptostrategy.platform.backtesting.api.model.Trade;
import com.cryptostrategy.platform.backtesting.api.model.TradeId;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.AttemptId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

public final class F006DatabaseFixture {
    public static final String EXPERIMENT = "60000000000000000000000001";
    public static final String CANDIDATE = "60000000000000000000000002";
    public static final String JOB = "60000000000000000000000003";
    public static final String ATTEMPT = "60000000000000000000000004";
    public static final String RESULT = "60000000000000000000000005";

    private static final Instant FIRST_ENTRY = Instant.parse("2026-01-01T00:00:00Z");
    private static final String[] TRADE_IDS = {
        "60000000000000000000000006",
        "60000000000000000000000007",
        "60000000000000000000000008",
        "60000000000000000000000009",
        "6000000000000000000000000A"
    };

    private F006DatabaseFixture() {}

    public static DataSource dataSource() {
        return new DriverManagerDataSource(
                System.getenv("DATABASE_URL"),
                System.getenv("DATABASE_USERNAME"),
                System.getenv("DATABASE_PASSWORD"));
    }

    public static TransactionTemplate transaction(DataSource source) {
        return new TransactionTemplate(new DataSourceTransactionManager(source));
    }

    public static void seed(JdbcTemplate jdbc) {
        jdbc.update(
                "insert into auth.users(id) values (?)",
                UUID.fromString("90000000-0000-4000-8000-000000000001"));
        jdbc.update(
                "insert into experiment.experiment(experiment_id,owner_user_id,name,status) values (?,?,?,?)",
                EXPERIMENT,
                UUID.fromString("90000000-0000-4000-8000-000000000001"),
                "F006 integration",
                "RUNNING");
        jdbc.update(
                "insert into experiment.candidate_definition(candidate_id,experiment_id,generation_index,definition,fingerprint) values (?,?,0,'{}','candidate-f006')",
                CANDIDATE,
                EXPERIMENT);
        jdbc.update(
                "insert into experiment.job(job_id,experiment_id,candidate_id,job_type,status,correlation_id,total_work,completed_work,failed_work) values (?,?,?,'BACKTEST','SUCCEEDED',?,1,1,0)",
                JOB,
                EXPERIMENT,
                CANDIDATE,
                JOB);
        jdbc.update(
                "insert into experiment.execution_attempt(attempt_id,job_id,candidate_id,attempt_no,status,finished_at) values (?,?,?,1,'SUCCEEDED',now())",
                ATTEMPT,
                JOB,
                CANDIDATE);
    }

    public static BacktestResult result() {
        BacktestResultId resultId = new BacktestResultId(RESULT);
        List<Trade> trades = new ArrayList<>();
        for (int sequence = 0; sequence < TRADE_IDS.length; sequence++) {
            trades.add(profitableTrade(resultId, sequence));
        }

        return new BacktestResult(
                resultId,
                new ExperimentId(EXPERIMENT),
                new CandidateId(CANDIDATE),
                new JobId(JOB),
                new AttemptId(ATTEMPT),
                new BacktestProvenance("manifest-fp", "dataset-fp", "strategy-fp"),
                BacktestAssumptions.mvp(
                        new BigDecimal("1000"), new BigDecimal("0.001"), BigDecimal.ZERO),
                Money.of(new BigDecimal("1000")),
                Money.of(new BigDecimal("1048.95")),
                Money.of(new BigDecimal("1.05")),
                trades,
                new EquityCurveSummary(
                        6,
                        Money.of(new BigDecimal("1048.95")),
                        Money.of(new BigDecimal("1048.95")),
                        5,
                        5,
                        "sha256:" + "1".repeat(64)),
                "sha256:" + "2".repeat(64),
                Instant.parse("2026-01-01T01:00:00Z"));
    }

    private static Trade profitableTrade(BacktestResultId resultId, int sequence) {
        Instant entryTime = FIRST_ENTRY.plusSeconds(sequence * 120L);
        BigDecimal postTradeCash = new BigDecimal("1000")
                .add(new BigDecimal("9.79").multiply(BigDecimal.valueOf(sequence + 1L)));
        return new Trade(
                new TradeId(TRADE_IDS[sequence]),
                resultId,
                sequence,
                PositionSide.LONG,
                entryTime,
                entryTime.plusSeconds(60),
                Money.of(new BigDecimal("100")),
                Money.of(new BigDecimal("110")),
                new Quantity(BigDecimal.ONE),
                Money.of(new BigDecimal("0.10")),
                Money.of(new BigDecimal("0.11")),
                Money.of(new BigDecimal("0.21")),
                new BigDecimal("9.79"),
                Money.of(postTradeCash),
                ExitReason.STRATEGY_SELL);
    }
}

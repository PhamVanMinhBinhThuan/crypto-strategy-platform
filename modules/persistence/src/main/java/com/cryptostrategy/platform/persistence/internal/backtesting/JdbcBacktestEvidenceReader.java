package com.cryptostrategy.platform.persistence.internal.backtesting;

import com.cryptostrategy.platform.backtesting.api.model.*;
import com.cryptostrategy.platform.backtesting.api.port.out.BacktestResultReader;
import com.cryptostrategy.platform.experiment.api.*;
import com.cryptostrategy.platform.experiment.api.job.*;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Reads the complete immutable Backtest evidence graph for reproduction. */
public final class JdbcBacktestEvidenceReader implements BacktestResultReader {
    private final JdbcTemplate jdbc;
    private final BacktestJsonMapper json;

    public JdbcBacktestEvidenceReader(JdbcTemplate jdbc, BacktestJsonMapper json) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.json = Objects.requireNonNull(json);
    }

    @Override public Optional<BacktestResult> findById(BacktestResultId id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("""
                    select experiment_id,candidate_id,job_id,successful_attempt_id,
                           manifest_fingerprint,dataset_fingerprint,strategy_fingerprint,
                           assumptions_json::text,initial_capital,final_capital,total_fees,
                           equity_point_count,equity_peak,equity_trough,equity_peak_sequence,
                           equity_trough_sequence,equity_curve_fingerprint,result_fingerprint,completed_at
                    from experiment.backtest_result where backtest_result_id=?
                    """, (rs, row) -> {
                List<Trade> trades = readTrades(id);
                return new BacktestResult(id, new ExperimentId(rs.getString(1)), new CandidateId(rs.getString(2)),
                        new JobId(rs.getString(3)), new AttemptId(rs.getString(4)),
                        new BacktestProvenance(rs.getString(5), rs.getString(6), rs.getString(7)),
                        json.read(rs.getString(8)), Money.of(rs.getBigDecimal(9)), Money.of(rs.getBigDecimal(10)),
                        Money.of(rs.getBigDecimal(11)), trades,
                        new EquityCurveSummary(rs.getLong(12), Money.of(rs.getBigDecimal(13)),
                                Money.of(rs.getBigDecimal(14)), rs.getLong(15), rs.getLong(16), rs.getString(17)),
                        rs.getString(18), rs.getTimestamp(19).toInstant());
            }, id.value()));
        } catch (EmptyResultDataAccessException absent) { return Optional.empty(); }
    }

    @Override
    public Optional<BacktestResult> findByJobId(JobId jobId) {
        try {
            BacktestResultId resultId = jdbc.queryForObject(
                    "select backtest_result_id from experiment.backtest_result where job_id=?",
                    (rs, row) -> new BacktestResultId(rs.getString(1)),
                    jobId.value());
            return resultId == null ? Optional.empty() : findById(resultId);
        } catch (EmptyResultDataAccessException absent) {
            return Optional.empty();
        }
    }

    @Override
    public List<BacktestResultSummary> listRecent(
            UUID ownerUserId, java.time.Instant beforeCompletedAt, String beforeResultId, int limit) {
        java.sql.Timestamp boundary = beforeCompletedAt == null
                ? null : java.sql.Timestamp.from(beforeCompletedAt);
        return jdbc.query("""
                select br.backtest_result_id, br.experiment_id, br.candidate_id,
                       c.generation_index, e.name as experiment_name,
                       c.definition::text as candidate_definition,
                       coalesce(er.total_return,
                           (br.final_capital - br.initial_capital) / nullif(br.initial_capital, 0), 0) as total_return,
                       coalesce(er.win_rate,
                           (select coalesce(sum(case when t.profit_loss > 0 then 1 else 0 end)::numeric
                               / nullif(count(*), 0), 0)
                            from experiment.trade t where t.backtest_result_id = br.backtest_result_id), 0) as win_rate,
                       coalesce(er.maximum_drawdown,
                           (br.equity_peak - br.equity_trough) / nullif(br.equity_peak, 0), 0) as maximum_drawdown,
                       coalesce(er.number_of_trades,
                           (select count(*) from experiment.trade t
                            where t.backtest_result_id = br.backtest_result_id), 0) as number_of_trades,
                       er.overall_score, br.completed_at
                from experiment.backtest_result br
                join experiment.experiment e on e.experiment_id = br.experiment_id
                join experiment.candidate_definition c on c.candidate_id = br.candidate_id
                left join lateral (
                    select evaluation.total_return, evaluation.win_rate,
                           evaluation.maximum_drawdown, evaluation.number_of_trades,
                           evaluation.overall_score
                    from experiment.evaluation_result evaluation
                    where evaluation.backtest_result_id = br.backtest_result_id
                    order by evaluation.evaluated_at desc, evaluation.evaluation_result_id desc
                    limit 1
                ) er on true
                where e.owner_user_id = ?
                  and (cast(? as timestamptz) is null
                       or (br.completed_at, br.backtest_result_id)
                          < (cast(? as timestamptz), ?))
                order by br.completed_at desc, br.backtest_result_id desc
                limit ?
                """, (rs, row) -> new BacktestResultSummary(
                        new BacktestResultId(rs.getString("backtest_result_id")),
                        new ExperimentId(rs.getString("experiment_id")),
                        new CandidateId(rs.getString("candidate_id")),
                        rs.getInt("generation_index"),
                        rs.getString("experiment_name"),
                        json.readMap(rs.getString("candidate_definition")),
                        rs.getBigDecimal("total_return"),
                        rs.getBigDecimal("win_rate"),
                        rs.getBigDecimal("maximum_drawdown"),
                        rs.getInt("number_of_trades"),
                        rs.getBigDecimal("overall_score"),
                        rs.getTimestamp("completed_at").toInstant()),
                ownerUserId, boundary, boundary, beforeResultId, limit);
    }

    @Override
    public long count(UUID ownerUserId) {
        Long count = jdbc.queryForObject("""
                select count(*)
                from experiment.backtest_result br
                join experiment.experiment e on e.experiment_id = br.experiment_id
                where e.owner_user_id = ?
                """, Long.class, ownerUserId);
        return count == null ? 0 : count;
    }

    private List<Trade> readTrades(BacktestResultId resultId) {
        return jdbc.query("""
                select trade_id,sequence_no,side,entry_time,exit_time,entry_price,exit_price,quantity,
                       entry_fee,exit_fee,fee,profit_loss,post_trade_cash,exit_reason
                from experiment.trade where backtest_result_id=? order by sequence_no
                """, (rs, row) -> new Trade(new TradeId(rs.getString(1)), resultId, rs.getInt(2),
                        PositionSide.valueOf(rs.getString(3).equals("BUY") ? "LONG" : rs.getString(3)),
                        rs.getTimestamp(4).toInstant(), rs.getTimestamp(5).toInstant(),
                        Money.of(rs.getBigDecimal(6)), Money.of(rs.getBigDecimal(7)), new Quantity(rs.getBigDecimal(8)),
                        Money.of(rs.getBigDecimal(9)), Money.of(rs.getBigDecimal(10)), Money.of(rs.getBigDecimal(11)),
                        rs.getBigDecimal(12), Money.of(rs.getBigDecimal(13)), ExitReason.valueOf(rs.getString(14))),
                resultId.value());
    }
}

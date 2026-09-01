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

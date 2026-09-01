package com.cryptostrategy.platform.persistence.internal.backtesting;

import com.cryptostrategy.platform.backtesting.api.error.BacktestErrorCode;
import com.cryptostrategy.platform.backtesting.api.error.BacktestException;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.backtesting.api.model.Trade;
import com.cryptostrategy.platform.backtesting.api.port.out.BacktestResultStore;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public final class JdbcBacktestResultStore implements BacktestResultStore {
    private final JdbcTemplate jdbc; private final TransactionTemplate tx; private final BacktestJsonMapper json;
    public JdbcBacktestResultStore(JdbcTemplate jdbc, TransactionTemplate tx, BacktestJsonMapper json) {
        this.jdbc=Objects.requireNonNull(jdbc); this.tx=Objects.requireNonNull(tx); this.json=Objects.requireNonNull(json);
    }
    @Override public BacktestResult save(BacktestResult result) {
        return tx.execute(status -> {
            List<String> attempts=jdbc.query("select a.attempt_id from experiment.candidate_definition c join experiment.job j on j.candidate_id=c.candidate_id and j.experiment_id=c.experiment_id join experiment.execution_attempt a on a.job_id=j.job_id and a.candidate_id=c.candidate_id where c.candidate_id=? and c.experiment_id=? and j.job_id=? and j.job_type='BACKTEST' and a.attempt_id=? and a.status='SUCCEEDED' for update of a",(rs,n)->rs.getString(1),result.candidateId().value(),result.experimentId().value(),result.jobId().value(),result.successfulAttemptId().value());
            if(attempts.size()!=1) throw new BacktestException(BacktestErrorCode.INVALID_LINEAGE,"Result lineage is not a successful BACKTEST attempt");
            BacktestResult existing=findByCandidate(result);
            if(existing!=null) return equivalent(existing,result);
            try { insert(result); return result; }
            catch(DuplicateKeyException concurrent) {
                BacktestResult canonical=findByCandidate(result);
                if(canonical==null) throw concurrent;
                return equivalent(canonical,result);
            }
        });
    }
    private BacktestResult findByCandidate(BacktestResult requested) {
        List<String> ids=jdbc.query("select backtest_result_id from experiment.backtest_result where candidate_id=?",(rs,n)->rs.getString(1),requested.candidateId().value());
        return ids.isEmpty()?null:new JdbcBacktestEvidenceReader(jdbc,json).findById(new BacktestResultId(ids.getFirst())).orElseThrow();
    }
    private static BacktestResult equivalent(BacktestResult canonical, BacktestResult requested) {
        boolean sameContent=canonical.fingerprint().equals(requested.fingerprint())
                && canonical.provenance().equals(requested.provenance())
                && canonical.assumptions().equals(requested.assumptions())
                && canonical.initialCapital().equals(requested.initialCapital())
                && canonical.finalCapital().equals(requested.finalCapital())
                && canonical.totalFees().equals(requested.totalFees())
                && canonical.equityCurveSummary().equals(requested.equityCurveSummary())
                && semanticTrades(canonical).equals(semanticTrades(requested));
        if(sameContent) return canonical;
        throw new BacktestException(BacktestErrorCode.DUPLICATE_OUTCOME,"Candidate already has a different Result");
    }
    private static List<String> semanticTrades(BacktestResult value){return value.trades().stream().map(t->t.sequence()+"|"+t.entryTime()+"|"+t.exitTime()+"|"+t.entryPrice()+"|"+t.exitPrice()+"|"+t.quantity()+"|"+t.entryFee()+"|"+t.exitFee()+"|"+t.realizedPnl()+"|"+t.postTradeCash()+"|"+t.exitReason()).toList();}
    private void insert(BacktestResult r) {
        var e=r.equityCurveSummary();
        jdbc.update("insert into experiment.backtest_result(backtest_result_id,experiment_id,candidate_id,job_id,successful_attempt_id,initial_capital,final_capital,total_fees,result_fingerprint,manifest_fingerprint,dataset_fingerprint,strategy_fingerprint,assumptions_version,assumptions_json,equity_point_count,equity_peak,equity_trough,equity_peak_sequence,equity_trough_sequence,equity_curve_fingerprint,completed_at) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?,?,?,?,?,?,?)",r.resultId().value(),r.experimentId().value(),r.candidateId().value(),r.jobId().value(),r.successfulAttemptId().value(),r.initialCapital().value(),r.finalCapital().value(),r.totalFees().value(),r.fingerprint(),r.provenance().manifestFingerprint(),r.provenance().datasetFingerprint(),r.provenance().strategyFingerprint(),r.assumptions().contractVersion(),json.write(r.assumptions()),e.pointCount(),e.peakEquity().value(),e.troughEquity().value(),e.peakSequence(),e.troughSequence(),e.curveDigest(),Timestamp.from(r.completedAt()));
        for(Trade t:r.trades()) jdbc.update("insert into experiment.trade(trade_id,backtest_result_id,sequence_no,side,entry_time,exit_time,entry_price,exit_price,quantity,entry_fee,exit_fee,fee,profit_loss,post_trade_cash,exit_reason) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",t.tradeId().value(),r.resultId().value(),t.sequence(),"BUY",Timestamp.from(t.entryTime()),Timestamp.from(t.exitTime()),t.entryPrice().value(),t.exitPrice().value(),t.quantity().value(),t.entryFee().value(),t.exitFee().value(),t.totalFee().value(),t.realizedPnl(),t.postTradeCash().value(),t.exitReason().name());
    }
}

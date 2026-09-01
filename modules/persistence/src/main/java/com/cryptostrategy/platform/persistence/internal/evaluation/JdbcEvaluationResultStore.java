package com.cryptostrategy.platform.persistence.internal.evaluation;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.evaluation.api.model.*;
import com.cryptostrategy.platform.evaluation.api.port.out.EvaluationResultStore;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public final class JdbcEvaluationResultStore implements EvaluationResultStore {
    private final JdbcTemplate jdbc; private final TransactionTemplate tx;
    public JdbcEvaluationResultStore(JdbcTemplate jdbc,TransactionTemplate tx){this.jdbc=Objects.requireNonNull(jdbc);this.tx=Objects.requireNonNull(tx);}
    @Override public EvaluationResult save(EvaluationResult result){
        try {
            return tx.execute(s->{
                Integer valid=jdbc.queryForObject("select count(*) from experiment.backtest_result where backtest_result_id=? and experiment_id=?",Integer.class,result.backtestResultId().value(),result.experimentId().value());
                if(valid==null||valid!=1)throw new IllegalArgumentException("Evaluation/Result Experiment mismatch");
                EvaluationResult existing=findCanonical(result); if(existing!=null)return equivalent(existing,result);
                insert(result);
                return result;
            });
        } catch(DuplicateKeyException conflict) {
            EvaluationResult canonical=findCanonical(result);
            if(canonical==null)throw conflict;
            return equivalent(canonical,result);
        }
    }
    private EvaluationResult findCanonical(EvaluationResult key){List<EvaluationResult> rows=jdbc.query("select evaluation_result_id,experiment_id,backtest_result_id,metric_version,ranking_version,total_return,win_rate,maximum_drawdown,number_of_trades,overall_score,leaderboard_eligible,evaluation_fingerprint,evaluated_at from experiment.evaluation_result where backtest_result_id=? and metric_version=? and ranking_version=?",(rs,n)->new EvaluationResult(new EvaluationResultId(rs.getString(1)),new ExperimentId(rs.getString(2)),new BacktestResultId(rs.getString(3)),new MetricVersion(rs.getString(4)),new RankingVersion(rs.getString(5)),rs.getBigDecimal(6),rs.getBigDecimal(7),rs.getBigDecimal(8),rs.getInt(9),rs.getBigDecimal(10),rs.getBoolean(11),rs.getString(12),rs.getTimestamp(13).toInstant()),key.backtestResultId().value(),key.metricVersion().value(),key.rankingVersion().value());return rows.isEmpty()?null:rows.getFirst();}
    private static EvaluationResult equivalent(EvaluationResult canonical,EvaluationResult requested){boolean same=canonical.fingerprint().equals(requested.fingerprint())&&canonical.experimentId().equals(requested.experimentId())&&canonical.backtestResultId().equals(requested.backtestResultId())&&canonical.metricVersion().equals(requested.metricVersion())&&canonical.rankingVersion().equals(requested.rankingVersion())&&canonical.totalReturn().equals(requested.totalReturn())&&canonical.winRate().equals(requested.winRate())&&canonical.maximumDrawdown().equals(requested.maximumDrawdown())&&canonical.numberOfTrades()==requested.numberOfTrades()&&canonical.overallScore().equals(requested.overallScore())&&canonical.leaderboardEligible()==requested.leaderboardEligible();if(same)return canonical;throw new IllegalStateException("Evaluation idempotency conflict");}
    private void insert(EvaluationResult r){BigDecimalParts scores=BigDecimalParts.from(r);jdbc.update("insert into experiment.evaluation_result(evaluation_result_id,experiment_id,backtest_result_id,metric_version,ranking_version,total_return,win_rate,maximum_drawdown,number_of_trades,return_score,win_rate_score,drawdown_score,overall_score,leaderboard_eligible,evaluation_fingerprint,evaluated_at) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",r.evaluationResultId().value(),r.experimentId().value(),r.backtestResultId().value(),r.metricVersion().value(),r.rankingVersion().value(),r.totalReturn(),r.winRate(),r.maximumDrawdown(),r.numberOfTrades(),scores.returnScore,scores.winScore,scores.drawdownScore,r.overallScore(),r.leaderboardEligible(),r.fingerprint(),Timestamp.from(r.evaluatedAt()));}
    private record BigDecimalParts(java.math.BigDecimal returnScore,java.math.BigDecimal winScore,java.math.BigDecimal drawdownScore){static BigDecimalParts from(EvaluationResult r){return new BigDecimalParts(r.totalReturn().max(java.math.BigDecimal.ZERO).min(java.math.BigDecimal.ONE),r.winRate(),java.math.BigDecimal.ONE.subtract(r.maximumDrawdown().max(java.math.BigDecimal.ZERO).min(java.math.BigDecimal.ONE)));}}
}

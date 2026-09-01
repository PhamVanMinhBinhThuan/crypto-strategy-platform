package com.cryptostrategy.platform.persistence.backtesting;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.backtesting.api.model.*;
import com.cryptostrategy.platform.persistence.api.BacktestingPersistenceFactory;
import com.cryptostrategy.platform.persistence.support.F006DatabaseFixture;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
class JdbcBacktestResultStoreIntegrationTest {
 @Test void atomicallyPersistsReadsAndReturnsCanonicalIdForEquivalentRetry(){
  var source=F006DatabaseFixture.dataSource();var tx=F006DatabaseFixture.transaction(source);
  tx.executeWithoutResult(status->{status.setRollbackOnly();var jdbc=new JdbcTemplate(source);F006DatabaseFixture.seed(jdbc);
   var factory=new BacktestingPersistenceFactory(source);var result=F006DatabaseFixture.result();assertEquals(result,factory.createResultStore().save(result));
   var retryId=new BacktestResultId("6000000000000000000000000Z");var retryTrades=new ArrayList<Trade>();
   for(var trade:result.trades())retryTrades.add(new Trade(new TradeId(String.format("700000000000000000000000%02d",trade.sequence()+1)),retryId,trade.sequence(),trade.side(),trade.entryTime(),trade.exitTime(),trade.entryPrice(),trade.exitPrice(),trade.quantity(),trade.entryFee(),trade.exitFee(),trade.totalFee(),trade.realizedPnl(),trade.postTradeCash(),trade.exitReason()));
   var retry=new BacktestResult(retryId,result.experimentId(),result.candidateId(),result.jobId(),result.successfulAttemptId(),result.provenance(),result.assumptions(),result.initialCapital(),result.finalCapital(),result.totalFees(),retryTrades,result.equityCurveSummary(),result.fingerprint(),result.completedAt());
   assertEquals(result.resultId(),factory.createResultStore().save(retry).resultId());
   assertEquals(1,jdbc.queryForObject("select count(*) from experiment.backtest_result where candidate_id=?",Integer.class,F006DatabaseFixture.CANDIDATE));
   assertEquals(5,jdbc.queryForObject("select count(*) from experiment.trade where backtest_result_id=?",Integer.class,F006DatabaseFixture.RESULT));
  });
 }
}

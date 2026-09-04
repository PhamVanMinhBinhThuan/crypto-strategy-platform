package com.cryptostrategy.platform.backtesting.internal;

import com.cryptostrategy.platform.backtesting.api.error.*;
import com.cryptostrategy.platform.backtesting.api.model.*;
import com.cryptostrategy.platform.backtesting.internal.fingerprint.BacktestFingerprintV1;
import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.marketdata.api.model.*;
import com.cryptostrategy.platform.marketdata.api.port.out.DatasetCandleReader;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.*;
import java.math.*;
import java.time.Instant;
import java.util.*;

public final class DeterministicBacktestEngine {
    public BacktestResult run(ResolvedBacktestRun command, DatasetCandleReader reader, Strategy strategy) {
        Objects.requireNonNull(command);Objects.requireNonNull(reader);Objects.requireNonNull(strategy);
        BacktestResultId resultId=BacktestResultId.generate();List<Trade> trades=new ArrayList<>();EquityAccumulator equity=new EquityAccumulator();
        TradeExecutionPolicy execution=new TradeExecutionPolicy();StrategyExecutionSession session=new StrategyExecutionSession(command.dataset(),strategy,command.lookback());
        final class State { BigDecimal cash=command.assumptions().initialCapital().value();Position position;StrategySignal pending=StrategySignal.HOLD; }
        State state=new State();
        new DatasetBatchCursor(command,reader).forEach((candle,last)->{
            if(state.pending==StrategySignal.BUY&&state.position==null){state.position=execution.open(candle,state.cash,command.assumptions());state.cash=BigDecimal.ZERO.setScale(12);}
            else if(state.pending==StrategySignal.SELL&&state.position!=null){TradeExecutionPolicy.ClosedTrade closed=execution.close(resultId,trades.size(),state.position,candle.open(),candle.key().openTime(),ExitReason.STRATEGY_SELL,command.assumptions());trades.add(closed.trade());state.cash=closed.cash();state.position=null;}
            session.evaluate(candle).ifPresent(decision -> state.pending=decision.signal());
            if(last&&state.position!=null&&command.assumptions().forceCloseAtEnd()){TradeExecutionPolicy.ClosedTrade closed=execution.close(resultId,trades.size(),state.position,candle.close(),candle.closeTime(),ExitReason.FORCED_FINAL_CLOSE,command.assumptions());trades.add(closed.trade());state.cash=closed.cash();state.position=null;}
            BigDecimal marked=state.position==null?state.cash:state.cash.add(state.position.quantity().value().multiply(candle.close()));equity.add(marked);
        });
        EquityCurveSummary summary=equity.finish();Money finalCapital=Money.of(state.cash);Money fees=Money.of(trades.stream().map(t->t.totalFee().value()).reduce(BigDecimal.ZERO,BigDecimal::add));
        String fingerprint=new BacktestFingerprintV1().calculate(command,trades,finalCapital,summary);
        return new BacktestResult(resultId,command.experimentId(),command.candidateId(),command.jobId(),command.attemptId(),command.provenance(),command.assumptions(),command.assumptions().initialCapital(),finalCapital,fees,trades,summary,fingerprint,Instant.now());
    }
}

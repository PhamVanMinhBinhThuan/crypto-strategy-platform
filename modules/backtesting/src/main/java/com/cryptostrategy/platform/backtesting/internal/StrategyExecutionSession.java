package com.cryptostrategy.platform.backtesting.internal;

import com.cryptostrategy.platform.backtesting.api.error.*;
import com.cryptostrategy.platform.domain.api.market.Candle;
import com.cryptostrategy.platform.marketdata.api.model.DatasetSnapshot;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.*;
import java.util.*;

/** Builds closed, bounded rolling contexts for the frozen Strategy. */
final class StrategyExecutionSession {
    private final DatasetSnapshot dataset; private final Strategy strategy; private final int lookback;
    private final Map<String, StrategySupplementalInput> supplementalInputs;
    private final ArrayDeque<Candle> window=new ArrayDeque<>();
    StrategyExecutionSession(DatasetSnapshot dataset,Strategy strategy,int lookback){this(dataset,strategy,lookback,Map.of());}
    StrategyExecutionSession(DatasetSnapshot dataset,Strategy strategy,int lookback,Map<String, StrategySupplementalInput> supplementalInputs){this.dataset=Objects.requireNonNull(dataset);this.strategy=Objects.requireNonNull(strategy);if(lookback<1)throw new IllegalArgumentException("lookback");this.lookback=lookback;this.supplementalInputs=Map.copyOf(Objects.requireNonNull(supplementalInputs));}
    Optional<StrategyDecision> evaluate(Candle candle){window.addLast(candle);while(window.size()>lookback)window.removeFirst();if(window.size()<lookback)return Optional.empty();StrategyDecision decision=strategy.evaluate(new StrategyContext(dataset.tradingPair(),dataset.timeframe(),List.copyOf(window),candle.closeTime(),supplementalInputs));if(decision==null||!decision.occurredAt().equals(candle.closeTime()))throw new BacktestException(BacktestErrorCode.INVALID_STRATEGY,"Decision time mismatch");return Optional.of(decision);}
    int retainedCandleCount(){return window.size();}
}

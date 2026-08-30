package com.cryptostrategy.platform.combination.internal;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.Strategy;
import com.cryptostrategy.platform.strategy.api.model.*;
import com.cryptostrategy.platform.domain.api.market.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
class CompositeStrategyTest {
    private static final StrategyReference REF=new StrategyReference(new StrategyVersionId("01J00000000000000000000000"),new StrategyPluginId("composite"),new SemanticVersion(1,0,0));
    @Test void evaluatesComponentsThroughSharedContract(){Strategy buy=context->decision(StrategySignal.BUY);Strategy sell=context->decision(StrategySignal.SELL);assertThrows(IllegalArgumentException.class,()->new CompositeStrategy(REF,new MajorityVotePolicy(),List.of(buy)));CompositeStrategy composite=new CompositeStrategy(REF,new MajorityVotePolicy(),List.of(buy,sell));TradingPair pair=new TradingPair(new TradingPairId("01J00000000000000000000003"),new Asset(new AssetId("01J00000000000000000000001"),new AssetSymbol("BTC"),Optional.empty(),true),new Asset(new AssetId("01J00000000000000000000002"),new AssetSymbol("USDT"),Optional.empty(),true),true);StrategyDecision result=composite.evaluate(new StrategyContext(pair,Timeframe.ONE_MINUTE,List.of(),Instant.EPOCH));assertEquals(StrategySignal.HOLD,result.signal());}
    private static StrategyDecision decision(StrategySignal signal){return new StrategyDecision(signal,Instant.EPOCH,REF,"TEST","test",Map.of());}
}

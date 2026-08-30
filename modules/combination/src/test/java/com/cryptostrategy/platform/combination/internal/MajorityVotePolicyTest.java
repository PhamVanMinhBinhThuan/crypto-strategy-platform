package com.cryptostrategy.platform.combination.internal;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.model.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
class MajorityVotePolicyTest {
    private static final StrategyReference REF=new StrategyReference(new StrategyVersionId("01J00000000000000000000000"),new StrategyPluginId("fixture"),new SemanticVersion(1,0,0));
    @Test void majorityWinsAndTiesHold(){MajorityVotePolicy policy=new MajorityVotePolicy();assertEquals(StrategySignal.BUY,policy.combine(List.of(decision(StrategySignal.BUY),decision(StrategySignal.BUY),decision(StrategySignal.SELL))));assertEquals(StrategySignal.HOLD,policy.combine(List.of(decision(StrategySignal.BUY),decision(StrategySignal.SELL))));}
    private static StrategyDecision decision(StrategySignal signal){return new StrategyDecision(signal,Instant.EPOCH,REF,"TEST","test",Map.of());}
}

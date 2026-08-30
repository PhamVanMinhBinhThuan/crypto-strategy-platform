package com.cryptostrategy.platform.architecture.fixtures.strategyextension;
import com.cryptostrategy.platform.strategy.api.*;
import com.cryptostrategy.platform.strategy.api.model.*;
import com.cryptostrategy.platform.strategy.api.model.parameter.*;
import java.util.*;
public final class MacdStrategyPluginFixture implements StrategyPlugin {
    private final StrategyDescriptor descriptor=new StrategyDescriptor(new StrategyReference(new StrategyVersionId("01J00000000000000000000009"),new StrategyPluginId("macd"),new SemanticVersion(1,0,0)),"strategy-contract-v1","MACD","Test-only extension","TREND",Set.of(StrategySignal.BUY,StrategySignal.SELL,StrategySignal.HOLD),26,StrategyParameterSchema.empty(),"test-only-macd-v1");
    @Override public StrategyDescriptor descriptor(){return descriptor;}
    @Override public Strategy create(StrategyParameterSet parameters){return context->new StrategyDecision(StrategySignal.HOLD,context.evaluationTime(),descriptor.reference(),"MACD_FIXTURE","Test-only MACD",Map.of());}
}

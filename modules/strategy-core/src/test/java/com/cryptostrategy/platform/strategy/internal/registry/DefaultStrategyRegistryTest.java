package com.cryptostrategy.platform.strategy.internal.registry;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.*;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.*;
import com.cryptostrategy.platform.strategy.api.model.parameter.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class DefaultStrategyRegistryTest {
    @Test void rejectsDuplicatesAndResolvesDefaults(){StrategyPlugin plugin=plugin();assertThrows(StrategyException.class,()->new DefaultStrategyRegistry(List.of(plugin,plugin)));DefaultStrategyRegistry registry=new DefaultStrategyRegistry(List.of(plugin));assertEquals(1,registry.listAvailable().size());assertNotNull(registry.create(new StrategyPluginId("fixture"),new SemanticVersion(1,0,0),Map.of()));}
    private static StrategyPlugin plugin(){StrategyDescriptor descriptor=new StrategyDescriptor(new StrategyReference(new StrategyVersionId("01J00000000000000000000000"),new StrategyPluginId("fixture"),new SemanticVersion(1,0,0)),"strategy-contract-v1","Fixture","Fixture","TEST",Set.of(StrategySignal.HOLD),1,StrategyParameterSchema.empty(),"fixture-v1");return new StrategyPlugin(){public StrategyDescriptor descriptor(){return descriptor;}public Strategy create(StrategyParameterSet parameters){return context->new StrategyDecision(StrategySignal.HOLD,context.evaluationTime(),descriptor.reference(),"FIXTURE","Fixture",Map.of());}};}
}

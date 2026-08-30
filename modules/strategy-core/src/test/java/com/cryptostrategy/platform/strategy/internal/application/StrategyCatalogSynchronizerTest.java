package com.cryptostrategy.platform.strategy.internal.application;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.*;
import com.cryptostrategy.platform.strategy.api.model.*;
import com.cryptostrategy.platform.strategy.api.model.parameter.*;
import com.cryptostrategy.platform.strategy.api.port.out.StrategyCatalogStore;
import com.cryptostrategy.platform.strategy.internal.registry.DefaultStrategyRegistry;
import java.util.*;
import org.junit.jupiter.api.Test;
class StrategyCatalogSynchronizerTest {@Test void synchronizesEveryTrustedDescriptor(){List<StrategyDescriptor> stored=new ArrayList<>();StrategyPlugin plugin=plugin();new StrategyCatalogSynchronizer(new DefaultStrategyRegistry(List.of(plugin)),stored::add).synchronize();assertEquals(List.of(plugin.descriptor()),stored);}private static StrategyPlugin plugin(){StrategyDescriptor descriptor=new StrategyDescriptor(new StrategyReference(new StrategyVersionId("01J00000000000000000000000"),new StrategyPluginId("fixture"),new SemanticVersion(1,0,0)),"strategy-contract-v1","Fixture","Fixture","TEST",Set.of(StrategySignal.HOLD),1,StrategyParameterSchema.empty(),"fixture");return new StrategyPlugin(){public StrategyDescriptor descriptor(){return descriptor;}public Strategy create(StrategyParameterSet parameters){return context->null;}};}}

package com.cryptostrategy.platform.strategy.internal.application;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.model.*;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.user.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class CompositeUserStrategyServiceTest {@Test void flatCompositeRequiresTwoDistinctSystemVersions(){StrategyReference one=new StrategyReference(new StrategyVersionId("01J00000000000000000000001"),new StrategyPluginId("one"),new SemanticVersion(1,0,0));UserStrategyComponent component=new UserStrategyComponent(one,StrategyParameterSet.empty());assertThrows(IllegalArgumentException.class,()->new CompositeStrategyDraftSource(new CombinationPolicyId("majority-vote"),new SemanticVersion(1,0,0),StrategyParameterSet.empty(),List.of(component)));assertThrows(IllegalArgumentException.class,()->new CompositeStrategyDraftSource(new CombinationPolicyId("majority-vote"),new SemanticVersion(1,0,0),StrategyParameterSet.empty(),List.of(component,component)));}}

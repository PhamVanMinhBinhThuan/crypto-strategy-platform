package com.cryptostrategy.platform.strategy.api.model;
import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.user.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
class UserStrategyModelTest {@Test void archiveAndPublishedVersionAreOneWay(){Instant now=Instant.EPOCH;UUID owner=UUID.randomUUID();UserStrategy root=new UserStrategy(new UserStrategyId("01J00000000000000000000000"),owner,StrategyKind.SINGLE," Mine ","",UserStrategyStatus.ACTIVE,Optional.empty(),now,now);assertEquals("Mine",root.name());assertEquals(UserStrategyStatus.ARCHIVED,root.archive(now.plusSeconds(1)).status());SingleStrategyDraftSource source=new SingleStrategyDraftSource(new StrategyReference(new StrategyVersionId("01J00000000000000000000001"),new StrategyPluginId("fixture"),new SemanticVersion(1,0,0)),StrategyParameterSet.empty());UserStrategyVersion draft=new UserStrategyVersion(new UserStrategyVersionId("01J00000000000000000000002"),root.id(),1,StrategyKind.SINGLE,source,UserStrategyVersionStatus.DRAFT,"strategy-v1:x",Optional.empty(),now);UserStrategyVersion published=draft.publish(now);assertEquals(UserStrategyVersionStatus.PUBLISHED,published.status());assertThrows(IllegalStateException.class,()->published.publish(now));}}
